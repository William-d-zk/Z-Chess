/*
 * MIT License                                                                    
 *                                                                                
 * Copyright (c) 2016~2020 Z-Chess                                                
 *                                                                                
 * Permission is hereby granted, free of charge, to any person obtaining a copy   
 * of this software and associated documentation files (the "Software"), to deal  
 * in the Software without restriction, including without limitation the rights   
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell      
 * copies of the Software, and to permit persons to whom the Software is          
 * furnished to do so, subject to the following conditions:                       
 *                                                                                
 * The above copyright notice and this permission notice shall be included in all 
 * copies or substantial portions of the Software.                                
 *                                                                                
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR     
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,       
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE    
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER         
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,  
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE  
 * SOFTWARE.                                                                      
 */

package com.tgx.chess.bishop.biz.config;

import com.tgx.chess.bishop.biz.db.dao.ZUID;
import com.tgx.chess.king.base.inf.IPair;

import java.util.List;

public interface IClusterConfig
{

    List<IPair> getPeers();

    List<IPair> getGates();

    IPair getBind();

    Uid getUid();

    ZUID createZUID();

    class Uid
    {
        private int nodeId;
        private int idcId;
        private int clusterId;
        private int type;

        public int getNodeId()
        {
            return nodeId;
        }

        public void setNodeId(int nodeId)
        {
            this.nodeId = nodeId;
        }

        public int getIdcId()
        {
            return idcId;
        }

        public void setIdcId(int idcId)
        {
            this.idcId = idcId;
        }

        public int getClusterId()
        {
            return clusterId;
        }

        public void setClusterId(int clusterId)
        {
            this.clusterId = clusterId;
        }

        public int getType()
        {
            return type;
        }

        public void setType(int type)
        {
            this.type = type;
        }
    }
}