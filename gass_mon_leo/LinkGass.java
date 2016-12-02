/**
*    Copyright 2011, Big Switch Networks, Inc. 
*    Originally created by David Erickson, Stanford University
* 
*    Licensed under the Apache License, Version 2.0 (the "License"); you may
*    not use this file except in compliance with the License. You may obtain
*    a copy of the License at
*
*         http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
*    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
*    License for the specific language governing permissions and limitations
*    under the License.
**/

package net.floodlightcontroller.gass_mon_leo;

import com.fasterxml.jackson.annotation.JsonProperty;


import org.openflow.util.HexString;

public class LinkGass implements Comparable<LinkGass> {
    @JsonProperty("src-switch")
    private long src;
    @JsonProperty("src-port")
    private short srcPort;
    @JsonProperty("dst-switch")
    private long dst;
    @JsonProperty("dst-port")
    private short dstPort;
   // @JsonProperty("lk-rate")
   // private long linkRate;
    @JsonProperty("lk-dataRate")
    private long dataRate;
    @JsonProperty("lk-errorRate")
	private long errorRate;
    @JsonProperty("lk-delay")
	private long delay;
	

	//private long delay;

	







    public LinkGass(long srcId, short srcPort, long dstId, short dstPort) {
        this.src = srcId;
        this.srcPort = srcPort;
        this.dst = dstId;
        this.dstPort = dstPort;
    //    this.linkRate = 0;
        this.errorRate=0;
        this.dataRate=0;
    }
	
	 public LinkGass(long srcId, short srcPort, long dstId, short dstPort, long dataRate, long errorRate, long delay) {
        this.src = srcId;
        this.srcPort = srcPort;
        this.dst = dstId;
        this.dstPort = dstPort;
      //  this.linkRate = 0;
        this.errorRate=errorRate;
        this.dataRate=dataRate;
		this.delay=delay;
    }

    // Convenience method
    public LinkGass(long srcId, int srcPort, long dstId, int dstPort) {
        this.src = srcId;
        this.srcPort = (short) srcPort;
        this.dst = dstId;
        this.dstPort = (short) dstPort;
      //  this.linkRate = 0;
    }

    /*
     * Do not use this constructor. Used primarily for JSON
     * Serialization/Deserialization
     */
    public LinkGass() {
        super();
    }

    public long getSrc() {
        return src;
    }

    public short getSrcPort() {
        return srcPort;
    }

    public long getDst() {
        return dst;
    }

    public short getDstPort() {
        return dstPort;
    }
    
    //plus gass
  /*  public long getLinkRate() {
        return linkRate;
    }*/

    public void setSrc(long src) {
        this.src = src;
    }

    public void setSrcPort(short srcPort) {
        this.srcPort = srcPort;
    }

    public void setDst(long dst) {
        this.dst = dst;
    }

    public void setDstPort(short dstPort) {
        this.dstPort = dstPort;
    }

    //plus gass
   /* public void setLinkRate(long lrt) {
    	this.linkRate=lrt;
    }*/
    
    public long getDataRate() {
    	return dataRate;
    }
    

	public void setDataRate(long dataRate) {
		this.dataRate = dataRate;
		}



	public long getErrorRate() {
		return errorRate;
	}
	

	public void setErrorRate(long errorRate) {
		this.errorRate = errorRate;
	}


	public long getDelay() {
		return delay;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (dst ^ (dst >>> 32));
        result = prime * result + dstPort;
        result = prime * result + (int) (src ^ (src >>> 32));
        result = prime * result + srcPort;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LinkGass other = (LinkGass) obj;
        if (dst != other.dst)
            return false;
        if (dstPort != other.dstPort)
            return false;
        if (src != other.src)
            return false;
        if (srcPort != other.srcPort)
            return false;
        if (dataRate != other.dataRate)
            return false;
        if (errorRate != other.errorRate)
            return false;
        return true;
    }


    @Override
    public String toString() {
        return "Link [src=" + HexString.toHexString(this.src) 
                + " outPort="
                + (srcPort & 0xffff)
                + ", dst=" + HexString.toHexString(this.dst)
                + ", inPort="
                + (dstPort & 0xffff)
                + "]";
    }
    
    public String toKeyString() {
    	return (HexString.toHexString(this.src) + "|" +
    			(this.srcPort & 0xffff) + "|" +
    			HexString.toHexString(this.dst) + "|" +
    		    (this.dstPort & 0xffff) );
    }

    @Override
    public int compareTo(LinkGass a) {
        // compare link based on natural ordering - src id, src port, dst id, dst port
        if (this.getSrc() != a.getSrc())
            return (int) (this.getSrc() - a.getSrc());
        
        if (this.getSrcPort() != a.getSrcPort())
            return (int) (this.getSrc() - a.getSrc());
        
        if (this.getDst() != a.getDst())
            return (int) (this.getDst() - a.getDst());
        
        return this.getDstPort() - a.getDstPort();
    }
}

