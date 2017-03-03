---
services: hdinsight
platforms: java
author: blackmist
---

# hdinsight-storm-azure-data-lake-store

This project demonstrates how to use a Java-based Storm topology to write data to Azure Data Lake Store using the [HdfsBolt](http://storm.apache.org/javadoc/apidocs/org/apache/storm/hdfs/bolt/HdfsBolt.html) component, which is provided as part of Apache Storm.

__NOTE__: This will probably only work with HDInsight clusters, as it relies on some core-site entries that tell it how to talk to Azure Data Lake Store, as well as some of the server-side components (hadoop-client, hadoop-hdfs,) having support for communicating with Data Lake Store

Also, this project is based on an HDInsight 3.5 cluster.

## How it works

This uses the Storm-HDFS bolt to write data to Azure Data Lake Store. This is a standard bolt provided as part of Apache Storm. The URL you use for the bolt, as well as the server-side configuration and components, are where the magic happens.

The project uses the Flux framework, so the configuration happens in the `datalakewriter.yaml` file. Here are the bits that configure the Storm-HDFS bolt:

```yaml
components:
  - id: "syncPolicy"
    className: "org.apache.storm.hdfs.bolt.sync.CountSyncPolicy"
    constructorArgs:
      - 1000
  - id: "rotationPolicy"
    className: "org.apache.storm.hdfs.bolt.rotation.FileSizeRotationPolicy"
    constructorArgs:
      - 5
      - KB

  - id: "fileNameFormat"
    className: "org.apache.storm.hdfs.bolt.format.DefaultFileNameFormat"
    configMethods:
      - name: "withPath"
        args: ["${hdfs.write.dir}"]
      - name: "withExtension"
        args: [".txt"]

  - id: "recordFormat"
    className: "org.apache.storm.hdfs.bolt.format.DelimitedRecordFormat"
    configMethods:
      - name: "withFieldDelimiter"
        args: ["|"]

  - id: "rotationAction"
    className: "org.apache.storm.hdfs.common.rotation.MoveFileAction"
    configMethods:
      - name: "toDestination"
        args: ["${hdfs.dest.dir}"]

# bolt definitions
bolts:
  - id: "hdfs-bolt"
    className: "org.apache.storm.hdfs.bolt.HdfsBolt"
    configMethods:
      - name: "withConfigKey"
        args: ["hdfs.config"]
      - name: "withFsUrl"
        args: ["${hdfs.url}"]
      - name: "withFileNameFormat"
        args: [ref: "fileNameFormat"]
      - name: "withRecordFormat"
        args: [ref: "recordFormat"]
      - name: "withRotationPolicy"
        args: [ref: "rotationPolicy"]
      - name: "withSyncPolicy"
        args: [ref: "syncPolicy"]
    parallelism: 1
```

The rest of the topology is just "emit a value, count it" to generate data to write out to data lake store.

## Running this sample

You need the following to run this example:

* A Storm on HDInsight cluster (version 3.5)

* [Java JDK 1.9](https://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html) or highter

* [Maven 3.x](higher)

### Build the sample

1. Download the example project to your development environment.

2. From a command prompt, terminal, or shell session, change directories to the root of the downloaded project, and run the following commands to build and package the topology.

        mvn compile
        mvn package
    
    Once the build and packaging completes, there will be a new directory named `target`, that contains a file named `StormToDataLakeStore-1.0-SNAPSHOT.jar`. This contains the compiled topology.

###Deploy and run the topology

If you created a Linux-based Storm on HDInsight cluster, use the steps below to deploy and run the topology.

1. Use the following command to copy the topology to the HDInsight cluster. Replace __USER__ with the SSH user name you used when creating the cluster. Replace __CLUSTERNAME__ with the name of the cluster.

        scp target\StormToDataLakeStore-1.0-SNAPSHOT.jar USER@CLUSTERNAME-ssh.azurehdinsight.net:StormToDataLakeStore-1.0-SNAPSHOT.jar
    
    When prompted, enter the password used when creating the SSH user for the cluster. If you used a public key instead of a password, you may need to use the `-i` parameter to specify the path to the matching private key.
    
    Note: If you are using a Windows client for development, you may not have an `scp` command. If so, you can use `pscp`, which is available from [http://www.chiark.greenend.org.uk/~sgtatham/putty/download.html](http://www.chiark.greenend.org.uk/~sgtatham/putty/download.html).

2. Once the upload completes, use the following to connect to the HDInsight cluster using SSH. Replace __USER__ with the SSH user name you used when creating the cluster. Replace __CLUSTERNAME__ with the name of the cluster.

        ssh USER@CLUSTERNAME-ssh.azurehdinsight.net

    When prompted, enter the password used when creating the SSH user for the cluster. If you used a public key instead of a password, you may need to use the `-i` parameter to specify the path to the matching private key.
    
    Note: If you are using a Windows client for development, follow the information in [Connect to Linux-based HDInsight with SSH from Windows](https://azure.microsoft.com/documentation/articles/hdinsight-hadoop-linux-use-ssh-windows/) for information on using the PuTTY client to connect to the cluster.
    
3. Once connected, create a `dev.properties` file and use the following as the contents:

        hdfs.write.dir: /stormdata/
        hdfs.url: adl:///

    This file is used to configure the Data Lake Store and directory that data is written to. The above text assumes that Data Lake Store is the default storage for the cluster.

4. Use the following to start the topology:

        storm jar StormToDataLakeStore-1.0-SNAPSHOT.jar org.apache.storm.flux.Flux --remote -R /datalakewriter.yaml --filter dev.properties

###View output data

To view the data, use the following command:

    hdfs dfs -ls /stormdata/

This displays a list of files that were created by the topology.

If Data Lake Store is not the default storage for the cluster, use the following command to view the data:

    hdfs dfs -ls adl://MYDATALAKE.azuredatalakestore.net/stormdata/

In the previous command, replace __MYDATALAKE__ with the Data Lake Store account.

The following list is an example of the data retuned by the previous commands:

    Found 30 items
    -rw-r-----+  1 larryfr larryfr       5120 2017-03-03 19:13 /stormdata/hdfs-bolt-3-0-1488568403092.txt
    -rw-r-----+  1 larryfr larryfr       5120 2017-03-03 19:13 /stormdata/hdfs-bolt-3-1-1488568404567.txt
    -rw-r-----+  1 larryfr larryfr       5120 2017-03-03 19:13 /stormdata/hdfs-bolt-3-10-1488568408678.txt
    -rw-r-----+  1 larryfr larryfr       5120 2017-03-03 19:13 /stormdata/hdfs-bolt-3-11-1488568411636.txt
    -rw-r-----+  1 larryfr larryfr       5120 2017-03-03 19:13 /stormdata/hdfs-bolt-3-12-1488568411884.txt
    -rw-r-----+  1 larryfr larryfr       5120 2017-03-03 19:13 /stormdata/hdfs-bolt-3-13-1488568412603.txt
    -rw-r-----+  1 larryfr larryfr       5120 2017-03-03 19:13 /stormdata/hdfs-bolt-3-14-1488568415055.txt

## Next steps   

For more information on using this sample, see [https://azure.microsoft.com/documentation/articles/hdinsight-storm-write-data-lake-store](https://azure.microsoft.com/documentation/articles/hdinsight-storm-write-data-lake-store).

## Project code of conduct

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.