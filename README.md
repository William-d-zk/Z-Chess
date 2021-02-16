# Z-Chess

* 以Chess为寓意，按照远近关系做了module的命名

## Z-King

* 提供了以disruptor为基座的事件处理框架。
* 提供了基础的拓扑结构的ZUID设计，拥有复杂的分段结构，共64bit
* 提供了基于TimeWheel的大容量时间调度器，最小时钟分片为1秒钟，
* 为异步处理过程设计了进度【ZProgress】和返回包装结构【ZResponse】
* NTRU 加密算法组件
* 以及其他各种基础框架需要具备的基础设计。
