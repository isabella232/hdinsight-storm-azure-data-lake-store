package com.microsoft.example;

import org.apache.storm.hdfs.bolt.HdfsBolt;
import org.apache.storm.hdfs.bolt.format.DefaultFileNameFormat;
import org.apache.storm.hdfs.bolt.format.DelimitedRecordFormat;
import org.apache.storm.hdfs.bolt.format.FileNameFormat;
import org.apache.storm.hdfs.bolt.format.RecordFormat;
import org.apache.storm.hdfs.bolt.rotation.FileRotationPolicy;
import org.apache.storm.hdfs.bolt.rotation.FileSizeRotationPolicy;
import org.apache.storm.hdfs.bolt.rotation.FileSizeRotationPolicy.Units;
import org.apache.storm.hdfs.bolt.sync.CountSyncPolicy;
import org.apache.storm.hdfs.bolt.sync.SyncPolicy;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * This topology demonstrates how to write to Azure Data Lake
 */
public class StormToDataLakeStore {
  // Basic bolt for counting and grouping the values emitted by the spout
  public static class CountTick extends BaseBasicBolt {
    private static final long serialVersionUID = 1L;
    Integer count = 0;
    Integer grouping = 1000;
    // Handle grouping parameters
  	CountTick(Integer groupingParam){
  		if (groupingParam > grouping) {
        grouping = (groupingParam /1000) * 1000;
      }
    }
    // Process inbound data and emit outbound
    @Override
    public void execute(Tuple tuple, BasicOutputCollector collector) {
      count++ ;
      if (count >= 1000000) {
        collector.emit(new Values(count));
        count = 0;
      }
    }
    // Declare field to be emitted
    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
      declarer.declare(new Fields("partialCount"));
    }
  }

  // Basic bolt for aggregating counts from the CountTick bolt
  public static class FinalCount extends BaseBasicBolt {
    private static final long serialVersionUID = 1L;
    Integer count = 0;
    // Process inbound data and emit outbound
    @Override
    public void execute(Tuple tuple, BasicOutputCollector collector) {
      Integer increment = tuple.getInteger(0);      
      count+= increment;      
      collector.emit(new Values(count));
    }
    //Declare output fields
    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
      declarer.declare(new Fields("finalcount"));
    }
  }

  // Main, where we define/submit the topology
  public static void main(String[] args) throws Exception {
    // Create a new topology
    TopologyBuilder builder = new TopologyBuilder();
    Integer parallelism = 16;
    Integer grouping = 1000000;
    // Handle arguments
    if (args.length > 1){	
    	Integer threadCount = Integer.parseInt(args[1]);
    	if (threadCount > 0 ) {
    		parallelism = threadCount;    		
    	}
    }
    if (args.length > 2){	
    	Integer groupingParam = Integer.parseInt(args[2]);
    	if (groupingParam > 0 ) {
    		grouping = groupingParam;    		
    	}
    }
    
    //Set the spout
    builder.setSpout("tickgenerator", new TickSpout(), parallelism);
    //And the bolts that do counting
    builder.setBolt("partialcount", new CountTick(grouping), parallelism).shuffleGrouping("tickgenerator");
    builder.setBolt("finalcount", new FinalCount(), 1).globalGrouping("partialcount");

    // Here's the real point of this example; how to use HdfsBolt to
    // write to Azure Data Lake Store

    // 1. Create sync and rotation policies to control when data is synched
    //    (written) to the file system and when to roll over into a new file.
    SyncPolicy syncPolicy = new CountSyncPolicy(1000);
    FileRotationPolicy rotationPolicy = new FileSizeRotationPolicy(0.5f, Units.KB);
    // 2. Set the format. In this case, comma delimited
    RecordFormat recordFormat = new DelimitedRecordFormat().withFieldDelimiter(",");
    // 3. Set the directory name. In this case, '/stormdata/'
    FileNameFormat fileNameFormat = new DefaultFileNameFormat().withPath("/stormdata/");
    // 4. Create the bolt using the previously created settings,
    //    and also tell it the base URL to your Data Lake Store
    // NOTE! Replace 'MYDATALAKE' below with the name of your data lake store.
    HdfsBolt adlsBolt = new HdfsBolt()
		.withFsUrl("adl://MYDATALAKE.azuredatalakestore.net/")
      	.withRecordFormat(recordFormat)
      	.withFileNameFormat(fileNameFormat)
      	.withRotationPolicy(rotationPolicy)
      	.withSyncPolicy(syncPolicy);
    // 4. Give it a name and wire it up to the bolt it accepts data
    //    from. NOTE: The name used here is also used as part of the
    //    file name for the files written to Data Lake Store.
    builder.setBolt("ADLStoreBolt", adlsBolt, 1)
      .globalGrouping("finalcount");
    
    // Back to normal topology stuff; configuration and submitting
    Config conf = new Config();
    conf.setDebug(false);

    if (args != null && args.length > 0) {
      conf.setNumWorkers((parallelism * 2) + 1);
      StormSubmitter.submitTopology(args[0], conf, builder.createTopology());
    }
    // Note that the local bits probably won't work from a random
    // development machine, since you won't have the bits and configuration
    // to talk to Azure Data Lake in that environment.
    else {
      conf.setMaxTaskParallelism(3);

      LocalCluster cluster = new LocalCluster();
      cluster.submitTopology("countticks", conf, builder.createTopology());

      Thread.sleep(10000);

      cluster.shutdown();
    }
  }
}

