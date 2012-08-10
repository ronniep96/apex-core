/*
 *  Copyright (c) 2012 Malhar, Inc.
 *  All Rights Reserved.
 */
package com.malhartech.bufferserver;

import com.malhartech.bufferserver.Buffer.Data;
import com.malhartech.bufferserver.policy.GiveAll;
import com.malhartech.bufferserver.policy.Policy;
import com.malhartech.bufferserver.util.SerializedData;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LogicalNode represents a logical node in a DAG. Logical node can be split into multiple physical nodes. The type of the logical node groups the multiple
 * physical nodes together in a group.
 *
 * @author chetan
 */
public class LogicalNode implements DataListener
{
  private static final Logger logger = LoggerFactory.getLogger(LogicalNode.class.getName());
  private final String upstream;
  private final String group;
  private final HashSet<PhysicalNode> physicalNodes;
  private final HashSet<ByteBuffer> partitions;
  private final Policy policy;
  private final DataListIterator iterator;

  LogicalNode(String upstream, String group, Iterator<SerializedData> iterator, Policy policy)
  {
    this.upstream = upstream;
    this.group = group;
    this.policy = policy;
    this.physicalNodes = new HashSet<PhysicalNode>();
    this.partitions = new HashSet<ByteBuffer>();

    if (iterator instanceof DataListIterator) {
      this.iterator = (DataListIterator) iterator;
    }
    else {
      throw new IllegalArgumentException("iterator does not belong to DataListIterator class");
    }
  }

  public String getGroup()
  {
    return group;
  }

  public Iterator<SerializedData> getIterator()
  {
    return iterator;
  }

  public void addChannel(Channel channel)
  {
    PhysicalNode pn = new PhysicalNode(channel);
    if (!physicalNodes.contains(pn)) {
      physicalNodes.add(pn);
    }
  }

  public void removeChannel(Channel channel)
  {
    for (PhysicalNode pn : physicalNodes) {
      if (pn.getChannel() == channel) {
        physicalNodes.remove(pn);
        break;
      }
    }
  }

  public void addPartition(ByteBuffer partition)
  {
    partitions.add(partition);
  }

  public synchronized void catchUp(long startTime)
  {
    long baseMillis = 0;
    int interval = 0;
    /*
     * fast forward to catch up with the windowId without consuming
     */
    outer:
    while (iterator.hasNext()) {
      SerializedData data = iterator.next();
      switch (iterator.getType()) {
        case RESET_WINDOW:
          Data resetWindow = (Data) iterator.getData();
          baseMillis = (long) resetWindow.getWindowId() << 32;
          interval = resetWindow.getResetWindow().getWidth();
          if (interval <= 0) {
            logger.warn("Interval value set to non positive value = {}", interval);
          }
          GiveAll.getInstance().distribute(physicalNodes, data);
          break;

        case BEGIN_WINDOW:
          if (baseMillis + iterator.getWindowId() * interval >= startTime) {
            GiveAll.getInstance().distribute(physicalNodes, data);
            break outer;
          }
          break;
      }
    }

    if (iterator.hasNext()) {
      dataAdded(DataListener.NULL_PARTITION);
    }

  }

  public synchronized void dataAdded(ByteBuffer partition)
  {
    /*
     * consume as much data as you can before running out of steam
     */
    // my assumption is that one will never get blocked while writing
    // since the underlying blocking queue maintained by netty has infinite
    // capacity. we will have to double check though.
    if (partitions.isEmpty()) {
      while (iterator.hasNext()) {
        SerializedData data = iterator.next();
        switch (iterator.getType()) {
          case PARTITIONED_DATA:
          case SIMPLE_DATA:
            policy.distribute(physicalNodes, data);
            break;

          default:
            GiveAll.getInstance().distribute(physicalNodes, data);
            break;
        }
      }
    }
    else {
      while (iterator.hasNext()) {
        SerializedData data = iterator.next();
        switch (iterator.getType()) {
          case PARTITIONED_DATA:
            if (partitions.contains(((Data) iterator.getData()).getPartitionedData().getPartition().asReadOnlyByteBuffer())) {
              policy.distribute(physicalNodes, data);
            }
            break;

          case SIMPLE_DATA:
            break;

          default:
            GiveAll.getInstance().distribute(physicalNodes, data);
            break;
        }
      }
    }
  }

  public int getPartitions(Collection<ByteBuffer> partitions)
  {
    partitions.addAll(this.partitions);
    return partitions.size();
  }

  public final int getPhysicalNodeCount()
  {
    return physicalNodes.size();
  }

  /**
   * @return the upstream
   */
  public String getUpstream()
  {
    return upstream;
  }
}
