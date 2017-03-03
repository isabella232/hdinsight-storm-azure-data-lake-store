package com.microsoft.example;

import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import java.util.Map;

// Basic spout that just emits a value of 1.
public class TickSpout extends BaseRichSpout {
  private static final long serialVersionUID = 1L;
  SpoutOutputCollector _collector;

  @Override
  public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
    _collector = collector;
  }
  // Just emit a value of 1
  @Override
  public void nextTuple() {    
    _collector.emit(new Values(1));
  }

  @Override
  public void ack(Object id) {
  }

  @Override
  public void fail(Object id) {
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declare(new Fields("tick"));
  }

}