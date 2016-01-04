# hdinsight-storm-azure-data-lake-store

This project demonstrates how to use a Java-based Storm topology to write data to Azure Data Lake Store using the [HdfsBolt](http://storm.apache.org/javadoc/apidocs/org/apache/storm/hdfs/bolt/HdfsBolt.html) component, which is provided as part of Apache Storm.

__NOTE__: This will probably only work with HDInsight clusters, as it relies on some core-site entries that tell it how to talk to Azure Data Lake Store, as well as some of the server-side components (hadoop-client, hadoop-hdfs,) having support for communicating with Data Lake Store

## How it works

#How it works

This uses the Storm HdfsBolt, so nothing out of the ordinary there. The URL you use for the bolt, as well as the server-side configuration and components, are where the magic happens.

Here's the code that writes to data lake store:

    // 1. Create sync and rotation policies to control when data is synched
    //    (written) to the file system and when to roll over into a new file.
    SyncPolicy syncPolicy = new CountSyncPolicy(1000);
    FileRotationPolicy rotationPolicy = new FileSizeRotationPolicy(0.5f, Units.KB);
    // 2. Set the format. In this case, comma delimited
    RecordFormat recordFormat = new DelimitedRecordFormat().withFieldDelimiter(",");
    // 3. Set the directory name. In this case, '/stormdata/'
    FileNameFormat fileNameFormat = new DefaultFileNameFormat().withPath("/stormdata/");
    // 4. Create the bolt using the previously created settings,
    //    and also tell it the base URL to your Data Lake Store.
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
      
The rest of the topology is just "emit a value, count it" to generate data to write out to data lake store.

## Running this sample

Follow the steps in [https://azure.com/documentation/articles/hdinsight-storm-write-data-lake-store](https://azure.com/documentation/articles/hdinsight-storm-write-data-lake-store/) to create an HDInsight cluster that can use Azure Data Lake Store. This document also includes steps to build, deploy, and run the topology to the cluster.