# Modbus 协议适配器 Phase 1 实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 Modbus TCP/RTU 协议适配器 Phase 1，包括常用功能码 (0x01-0x06, 0x0F, 0x10)、主站/从站双模式、基础监控指标。

**Architecture:** 基于现有 ModbusTcpCodec 和 ModbusTcpProtocolHandler 扩展，新增 ModbusRtuCodec、主站/从站实现、标签系统、重试策略和监控指标。协议处理器通过 ProtocolHandler SPI 集成到 Z-Bishop 协议链。

**Tech Stack:** Java 17, Spring Boot 3.5.9, Z-Queen AIO, Z-King 基础组件，JUnit 5 测试框架。

---

## Chunk 1: 核心协议层

### Task 1: 扩展 ModbusExceptionCode 异常码枚举

**Files:**
- Modify: `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/model/ModbusExceptionCode.java`
- Test: `Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/modbus/model/ModbusExceptionCodeTest.java` (新建)

- [ ] **Step 1: 编写 ModbusExceptionCode 测试**

```java
package com.isahl.chess.bishop.protocol.modbus.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModbusExceptionCodeTest {
    
    @Test
    void testFromCode_validCodes() {
        assertEquals(ModbusExceptionCode.ILLEGAL_FUNCTION, ModbusExceptionCode.fromCode(0x01));
        assertEquals(ModbusExceptionCode.ILLEGAL_DATA_ADDRESS, ModbusExceptionCode.fromCode(0x02));
        assertEquals(ModbusExceptionCode.ILLEGAL_DATA_VALUE, ModbusExceptionCode.fromCode(0x03));
        assertEquals(ModbusExceptionCode.SERVER_DEVICE_FAILURE, ModbusExceptionCode.fromCode(0x04));
    }
    
    @Test
    void testFromCode_invalidCode_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            ModbusExceptionCode.fromCode(0x99);
        });
    }
    
    @Test
    void testGetCode_and getDescription() {
        ModbusExceptionCode code = ModbusExceptionCode.SERVER_DEVICE_BUSY;
        assertEquals(0x06, code.getCode());
        assertEquals("Server Device Busy", code.getDescription());
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
mvn test -Dtest=ModbusExceptionCodeTest -pl Z-Bishop
```
Expected: FAIL (测试方法不存在)

- [ ] **Step 3: 实现测试方法**

当前文件已有 `fromCode`、`getCode`、`getDescription` 方法，测试应该通过。

- [ ] **Step 4: 运行测试验证通过**

```bash
mvn test -Dtest=ModbusExceptionCodeTest -pl Z-Bishop
```
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/model/ModbusExceptionCode.java
git add Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/modbus/model/ModbusExceptionCodeTest.java
git commit -m "test: add ModbusExceptionCode unit tests"
```

---

### Task 2: 创建 ModbusException 异常类

**Files:**
- Create: `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/exception/ModbusException.java`
- Test: `Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/modbus/exception/ModbusExceptionTest.java` (新建)

- [ ] **Step 1: 编写 ModbusException 测试**

```java
package com.isahl.chess.bishop.protocol.modbus.exception;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.model.ModbusExceptionCode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModbusExceptionTest {
    
    @Test
    void testConstructor_withFunctionAndExceptionCode() {
        ModbusException ex = new ModbusException(
            ModbusFunction.READ_HOLDING_REGISTERS,
            ModbusExceptionCode.ILLEGAL_DATA_ADDRESS
        );
        
        assertEquals(ModbusFunction.READ_HOLDING_REGISTERS, ex.getFunction());
        assertEquals(ModbusExceptionCode.ILLEGAL_DATA_ADDRESS, ex.getExceptionCode());
        assertEquals(0x03, ex.getFunctionCode());
        assertEquals(0x02, ex.getExceptionCodeValue());
    }
    
    @Test
    void testConstructor_withMessage() {
        ModbusException ex = new ModbusException("Test message");
        assertEquals("Test message", ex.getMessage());
    }
    
    @Test
    void testIsExceptionResponse() {
        ModbusException ex = new ModbusException(
            ModbusFunction.READ_HOLDING_REGISTERS,
            ModbusExceptionCode.ILLEGAL_DATA_ADDRESS
        );
        assertTrue(ex.isExceptionResponse());
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
mvn test -Dtest=ModbusExceptionTest -pl Z-Bishop
```
Expected: FAIL (类不存在)

- [ ] **Step 3: 实现 ModbusException 类**

```java
package com.isahl.chess.bishop.protocol.modbus.exception;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.model.ModbusExceptionCode;

/**
 * Modbus 协议异常
 */
public class ModbusException extends RuntimeException {
    
    private final ModbusFunction function;
    private final ModbusExceptionCode exceptionCode;
    
    public ModbusException(ModbusFunction function, ModbusExceptionCode exceptionCode) {
        super(String.format("Modbus exception: function=0x%02X, exception=0x%02X (%s)",
            function.getCode(), exceptionCode.getCode(), exceptionCode.getDescription()));
        this.function = function;
        this.exceptionCode = exceptionCode;
    }
    
    public ModbusException(String message) {
        super(message);
        this.function = null;
        this.exceptionCode = null;
    }
    
    public ModbusFunction getFunction() {
        return function;
    }
    
    public ModbusExceptionCode getExceptionCode() {
        return exceptionCode;
    }
    
    public int getFunctionCode() {
        return function != null ? function.getCode() : -1;
    }
    
    public int getExceptionCodeValue() {
        return exceptionCode != null ? exceptionCode.getCode() : -1;
    }
    
    public boolean isExceptionResponse() {
        return true;
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
mvn test -Dtest=ModbusExceptionTest -pl Z-Bishop
```
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/exception/ModbusException.java
git add Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/modbus/exception/ModbusExceptionTest.java
git commit -m "feat: add ModbusException class for protocol errors"
```

---

### Task 3: 创建 ModbusRtuCodec RTU 编解码器

**Files:**
- Create: `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/rtu/ModbusRtuCodec.java`
- Test: `Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/modbus/rtu/ModbusRtuCodecTest.java` (新建)

- [ ] **Step 1: 编写 ModbusRtuCodec 测试**

```java
package com.isahl.chess.bishop.protocol.modbus.rtu;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.model.ModbusMessage;
import com.isahl.chess.king.base.content.ByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModbusRtuCodecTest {
    
    @Test
    void testEncodeReadHoldingRegisters() {
        ModbusMessage message = new ModbusMessage(1, ModbusFunction.READ_HOLDING_REGISTERS, new byte[]{0, 0, 0, 10});
        ByteBuf buffer = ModbusRtuCodec.encode(message);
        
        assertEquals(8, buffer.readableBytes()); // 1 address + 1 function + 4 data + 2 CRC
        assertEquals(1, buffer.peek(0) & 0xFF); // Unit ID
        assertEquals(0x03, buffer.peek(1) & 0xFF); // Function code
    }
    
    @Test
    void testDecodeReadHoldingRegisters() {
        byte[] data = {0x01, 0x03, 0x00, 0x00, 0x00, 0x0A, 0xC4, 0x0B}; // CRC = 0x0BC4 (little endian)
        ByteBuf buffer = ByteBuf.allocate(data.length);
        buffer.put(data);
        
        ModbusMessage message = ModbusRtuCodec.decode(buffer);
        
        assertNotNull(message);
        assertEquals(1, message.getUnitId());
        assertEquals(ModbusFunction.READ_HOLDING_REGISTERS, message.getFunction());
        assertEquals(4, message.getData().length);
    }
    
    @Test
    void testDecodeIncompleteData() {
        byte[] data = {0x01, 0x03};
        ByteBuf buffer = ByteBuf.allocate(data.length);
        buffer.put(data);
        
        assertNull(ModbusRtuCodec.decode(buffer));
    }
    
    @Test
    void testCrcValidation() {
        byte[] data = {0x01, 0x03, 0x00, 0x00, 0x00, 0x0A, (byte) 0xC4, 0x0B};
        ByteBuf buffer = ByteBuf.allocate(data.length);
        buffer.put(data);
        
        ModbusMessage message = ModbusRtuCodec.decode(buffer);
        assertNotNull(message); // CRC should be valid
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
mvn test -Dtest=ModbusRtuCodecTest -pl Z-Bishop
```
Expected: FAIL (类不存在)

- [ ] **Step 3: 实现 ModbusRtuCodec 类**

```java
package com.isahl.chess.bishop.protocol.modbus.rtu;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.model.ModbusMessage;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.util.CryptoUtil;

/**
 * Modbus RTU 协议编解码器
 */
public class ModbusRtuCodec {
    
    private static final int RTU_HEADER_LENGTH = 2; // Address + Function
    private static final int RTU_CRC_LENGTH = 2;
    
    /**
     * 编码 RTU 帧
     */
    public static ByteBuf encode(ModbusMessage message) {
        byte[] data = message.getData() != null ? message.getData() : new byte[0];
        int length = RTU_HEADER_LENGTH + data.length + RTU_CRC_LENGTH;
        
        byte[] buffer = new byte[length];
        buffer[0] = (byte) message.getUnitId();
        buffer[1] = (byte) message.getFunction().getCode();
        if (data.length > 0) {
            System.arraycopy(data, 0, buffer, 2, data.length);
        }
        
        // 计算 CRC (little endian)
        int crc = CryptoUtil.crc16_modbus(buffer, 0, length - 2);
        buffer[length - 2] = (byte) (crc & 0xFF);
        buffer[length - 1] = (byte) ((crc >> 8) & 0xFF);
        
        return ByteBuf.wrap(buffer);
    }
    
    /**
     * 解码 RTU 帧
     */
    public static ModbusMessage decode(ByteBuf buffer) {
        if (buffer.readableBytes() < RTU_HEADER_LENGTH + RTU_CRC_LENGTH) {
            return null;
        }
        
        int unitId = buffer.peek(0) & 0xFF;
        int functionCode = buffer.peek(1) & 0xFF;
        
        // 估算帧长度 (基于功能码)
        int estimatedLength = estimateFrameLength(functionCode, buffer);
        if (estimatedLength < 0 || buffer.readableBytes() < estimatedLength) {
            return null;
        }
        
        // 验证 CRC
        int crcLength = estimatedLength - RTU_CRC_LENGTH;
        int receivedCrc = (buffer.peek(estimatedLength - 2) & 0xFF) | 
                         ((buffer.peek(estimatedLength - 1) & 0xFF) << 8);
        int calculatedCrc = CryptoUtil.crc16_modbus(buffer.array(), 0, crcLength);
        
        if (receivedCrc != calculatedCrc) {
            throw new IllegalArgumentException("Modbus RTU CRC error");
        }
        
        // 解析数据
        byte[] data = new byte[crcLength - RTU_HEADER_LENGTH];
        for (int i = 0; i < data.length; i++) {
            data[i] = buffer.peek(2 + i);
        }
        
        ModbusFunction function = ModbusFunction.fromCode(functionCode);
        return new ModbusMessage(unitId, function, data);
    }
    
    /**
     * 估算帧长度
     */
    private static int estimateFrameLength(int functionCode, ByteBuf buffer) {
        // 基于功能码估算数据长度
        switch (functionCode) {
            case 0x01: // Read Coils
            case 0x02: // Read Discrete Inputs
            case 0x03: // Read Holding Registers
            case 0x04: // Read Input Registers
                return 8; // 2 header + 4 data + 2 CRC
            case 0x05: // Write Single Coil
            case 0x06: // Write Single Register
                return 8; // 2 header + 4 data + 2 CRC
            case 0x0F: // Write Multiple Coils
            case 0x10: // Write Multiple Registers
                if (buffer.readableBytes() >= 7) {
                    int byteCount = buffer.peek(6) & 0xFF;
                    return 2 + 5 + byteCount + 2;
                }
                return -1;
            default:
                return -1;
        }
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
mvn test -Dtest=ModbusRtuCodecTest -pl Z-Bishop
```
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/rtu/ModbusRtuCodec.java
git add Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/modbus/rtu/ModbusRtuCodecTest.java
git commit -m "feat: implement Modbus RTU codec with CRC validation"
```

---

### Task 4: 实现 ModbusRtuProtocolHandler RTU 协议处理器

**Files:**
- Create: `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/spi/ModbusRtuProtocolHandler.java`
- Test: `Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/modbus/spi/ModbusRtuProtocolHandlerTest.java` (新建)

- [ ] **Step 1: 编写 ModbusRtuProtocolHandler 测试**

```java
package com.isahl.chess.bishop.protocol.modbus.spi;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.model.ModbusMessage;
import com.isahl.chess.king.base.content.ByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModbusRtuProtocolHandlerTest {
    
    private final ModbusRtuProtocolHandler handler = new ModbusRtuProtocolHandler();
    
    @Test
    void testGetProtocolName() {
        assertEquals("ModbusRTU", handler.getProtocolName());
    }
    
    @Test
    void testGetProtocolSignature() {
        assertNull(handler.getProtocolSignature()); // RTU has no fixed signature
    }
    
    @Test
    void testDecode_and Encode() {
        ModbusMessage message = new ModbusMessage(1, ModbusFunction.READ_HOLDING_REGISTERS, new byte[]{0, 0, 0, 10});
        byte[] encoded = handler.encode(message);
        
        ByteBuf buffer = ByteBuf.allocate(encoded.length);
        buffer.put(encoded);
        
        Object decoded = handler.decode(buffer);
        assertNotNull(decoded);
        assertTrue(decoded instanceof ModbusMessage);
        
        ModbusMessage decodedMessage = (ModbusMessage) decoded;
        assertEquals(1, decodedMessage.getUnitId());
        assertEquals(ModbusFunction.READ_HOLDING_REGISTERS, decodedMessage.getFunction());
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
mvn test -Dtest=ModbusRtuProtocolHandlerTest -pl Z-Bishop
```
Expected: FAIL (类不存在)

- [ ] **Step 3: 实现 ModbusRtuProtocolHandler 类**

```java
package com.isahl.chess.bishop.protocol.modbus.spi;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.model.ModbusMessage;
import com.isahl.chess.bishop.protocol.modbus.rtu.ModbusRtuCodec;
import com.isahl.chess.bishop.protocol.spi.ProtocolHandler;
import com.isahl.chess.king.base.content.ByteBuf;

/**
 * Modbus RTU 协议处理器
 */
public class ModbusRtuProtocolHandler implements ProtocolHandler {
    
    @Override
    public String getProtocolName() {
        return "ModbusRTU";
    }
    
    @Override
    public String getProtocolVersion() {
        return "1.0.0";
    }
    
    @Override
    public byte[] getProtocolSignature() {
        return null; // RTU 无固定签名
    }
    
    @Override
    public Object decode(ByteBuf buffer) {
        return ModbusRtuCodec.decode(buffer);
    }
    
    @Override
    public byte[] encode(Object message) {
        if (!(message instanceof ModbusMessage)) {
            throw new IllegalArgumentException("Message must be a ModbusMessage");
        }
        
        ModbusMessage modbusMessage = (ModbusMessage) message;
        ByteBuf buffer = ModbusRtuCodec.encode(modbusMessage);
        
        byte[] result = new byte[buffer.readableBytes()];
        buffer.get(result);
        
        return result;
    }
    
    @Override
    public int getPriority() {
        return 400; // 低于 TCP
    }
    
    /**
     * 创建读保持寄存器请求
     */
    public ModbusMessage createReadHoldingRegisters(int unitId, int startAddress, int quantity) {
        byte[] data = new byte[4];
        data[0] = (byte) ((startAddress >> 8) & 0xFF);
        data[1] = (byte) (startAddress & 0xFF);
        data[2] = (byte) ((quantity >> 8) & 0xFF);
        data[3] = (byte) (quantity & 0xFF);
        
        return new ModbusMessage(unitId, ModbusFunction.READ_HOLDING_REGISTERS, data);
    }
    
    /**
     * 创建写单个寄存器请求
     */
    public ModbusMessage createWriteSingleRegister(int unitId, int address, int value) {
        byte[] data = new byte[4];
        data[0] = (byte) ((address >> 8) & 0xFF);
        data[1] = (byte) (address & 0xFF);
        data[2] = (byte) ((value >> 8) & 0xFF);
        data[3] = (byte) (value & 0xFF);
        
        return new ModbusMessage(unitId, ModbusFunction.WRITE_SINGLE_REGISTER, data);
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
mvn test -Dtest=ModbusRtuProtocolHandlerTest -pl Z-Bishop
```
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/spi/ModbusRtuProtocolHandler.java
git add Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/modbus/spi/ModbusRtuProtocolHandlerTest.java
git commit -m "feat: implement Modbus RTU protocol handler SPI"
```

---

## Chunk 2: 主站/从站实现

### Task 5: 创建 ModbusRequest 和 ModbusResponse 模型

**Files:**
- Create: `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/master/ModbusRequest.java`
- Create: `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/master/ModbusResponse.java`
- Test: `Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/modbus/master/ModbusRequestResponseTest.java` (新建)

- [ ] **Step 1: 编写测试**

```java
package com.isahl.chess.bishop.protocol.modbus.master;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModbusRequestResponseTest {
    
    @Test
    void testModbusRequest() {
        ModbusRequest request = new ModbusRequest(1, ModbusFunction.READ_HOLDING_REGISTERS, new byte[]{0, 0, 0, 10});
        
        assertEquals(1, request.getUnitId());
        assertEquals(ModbusFunction.READ_HOLDING_REGISTERS, request.getFunction());
        assertEquals(4, request.getData().length);
    }
    
    @Test
    void testModbusResponse() {
        ModbusResponse response = new ModbusResponse(1, ModbusFunction.READ_HOLDING_REGISTERS, new byte[]{0, 4, 0, 10, 0, 20});
        
        assertEquals(1, response.getUnitId());
        assertEquals(ModbusFunction.READ_HOLDING_REGISTERS, response.getFunction());
        assertEquals(6, response.getData().length);
        assertFalse(response.isException());
    }
    
    @Test
    void testModbusResponse_exception() {
        ModbusResponse response = ModbusResponse.exception(1, ModbusFunction.READ_HOLDING_REGISTERS, 0x02);
        
        assertTrue(response.isException());
        assertEquals(0x83, response.getFunctionCode());
        assertEquals(0x02, response.getExceptionCode());
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
mvn test -Dtest=ModbusRequestResponseTest -pl Z-Bishop
```
Expected: FAIL (类不存在)

- [ ] **Step 3: 实现 ModbusRequest 和 ModbusResponse 类**

```java
package com.isahl.chess.bishop.protocol.modbus.master;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.model.ModbusMessage;

/**
 * Modbus 请求
 */
public class ModbusRequest extends ModbusMessage {
    
    private final int transactionId;
    
    public ModbusRequest(int unitId, ModbusFunction function, byte[] data) {
        super(unitId, function, data);
        this.transactionId = -1;
    }
    
    public ModbusRequest(int transactionId, int unitId, ModbusFunction function, byte[] data) {
        super(unitId, function, data);
        this.transactionId = transactionId;
    }
    
    public int getTransactionId() {
        return transactionId;
    }
}
```

```java
package com.isahl.chess.bishop.protocol.modbus.master;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.model.ModbusMessage;

/**
 * Modbus 响应
 */
public class ModbusResponse extends ModbusMessage {
    
    private final int exceptionCode;
    
    public ModbusResponse(int unitId, ModbusFunction function, byte[] data) {
        super(unitId, function, data);
        this.exceptionCode = -1;
    }
    
    public ModbusResponse(int unitId, ModbusFunction function, byte[] data, int exceptionCode) {
        super(unitId, function, data);
        this.exceptionCode = exceptionCode;
    }
    
    public int getExceptionCode() {
        return exceptionCode;
    }
    
    public int getFunctionCode() {
        return getFunction().getCode();
    }
    
    @Override
    public boolean isException() {
        return exceptionCode >= 0 || ModbusFunction.isException(getFunctionCode());
    }
    
    /**
     * 创建异常响应
     */
    public static ModbusResponse exception(int unitId, ModbusFunction function, int exceptionCode) {
        ModbusFunction exceptionFunction = ModbusFunction.fromCode(function.getCode() | 0x80);
        return new ModbusResponse(unitId, exceptionFunction, new byte[]{(byte) exceptionCode}, exceptionCode);
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
mvn test -Dtest=ModbusRequestResponseTest -pl Z-Bishop
```
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/master/ModbusRequest.java
git add Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/master/ModbusResponse.java
git add Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/modbus/master/ModbusRequestResponseTest.java
git commit -m "feat: add ModbusRequest and ModbusResponse models"
```

---

### Task 6: 实现 ModbusMaster 主站类

**Files:**
- Create: `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/master/ModbusMaster.java`
- Test: `Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/modbus/master/ModbusMasterTest.java` (新建)

- [ ] **Step 1: 编写 ModbusMaster 测试**

```java
package com.isahl.chess.bishop.protocol.modbus.master;

import com.isahl.chess.bishop.protocol.modbus.retry.ExponentialBackoffRetry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModbusMasterTest {
    
    @Test
    void testBuilder() {
        ModbusMaster master = ModbusMaster.builder()
            .host("localhost")
            .port(502)
            .timeout(3000)
            .retryPolicy(new ExponentialBackoffRetry(3, 1000))
            .build();
        
        assertNotNull(master);
        assertFalse(master.isConnected());
    }
    
    @Test
    void testCreateReadHoldingRegistersRequest() {
        ModbusMaster master = ModbusMaster.builder().host("localhost").port(502).build();
        ModbusRequest request = master.createReadHoldingRegisters(1, 0, 10);
        
        assertEquals(1, request.getUnitId());
        assertEquals(4, request.getData().length);
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
mvn test -Dtest=ModbusMasterTest -pl Z-Bishop
```
Expected: FAIL (类不存在)

- [ ] **Step 3: 实现 ModbusMaster 类 (简化版，Phase 1)**

```java
package com.isahl.chess.bishop.protocol.modbus.master;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.retry.RetryPolicy;
import com.isahl.chess.bishop.protocol.modbus.tcp.ModbusTcpCodec;
import com.isahl.chess.king.base.content.ByteBuf;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Modbus 主站 (客户端)
 */
public class ModbusMaster {
    
    private final String host;
    private final int port;
    private final int timeoutMs;
    private final RetryPolicy retryPolicy;
    private AsynchronousSocketChannel channel;
    private final AtomicInteger transactionIdGenerator = new AtomicInteger(0);
    
    private ModbusMaster(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.timeoutMs = builder.timeoutMs;
        this.retryPolicy = builder.retryPolicy;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public void connect() throws IOException {
        channel = AsynchronousSocketChannel.open();
        channel.connect(new InetSocketAddress(host, port)).get(timeoutMs, TimeUnit.MILLISECONDS);
    }
    
    public void close() throws IOException {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
    }
    
    public boolean isConnected() {
        return channel != null && channel.isOpen();
    }
    
    public ModbusResponse sendRequest(ModbusRequest request) throws IOException, InterruptedException {
        int transactionId = nextTransactionId();
        
        // 编码
        ByteBuf buffer = ModbusTcpCodec.encode(request, transactionId);
        
        // 发送
        channel.write(java.nio.ByteBuffer.wrap(buffer.array())).get(timeoutMs, TimeUnit.MILLISECONDS);
        
        // 接收响应
        byte[] responseBuffer = new byte[256];
        int bytesRead = channel.read(java.nio.ByteBuffer.wrap(responseBuffer)).get(timeoutMs, TimeUnit.MILLISECONDS);
        
        // 解码
        ByteBuf responseByteBuf = ByteBuf.wrap(responseBuffer, 0, bytesRead);
        ModbusTcpCodec.ModbusTcpMessage response = ModbusTcpCodec.decode(responseByteBuf);
        
        return new ModbusResponse(response.getUnitId(), response.getFunction(), response.getData());
    }
    
    public ModbusRequest createReadHoldingRegisters(int unitId, int startAddress, int quantity) {
        byte[] data = new byte[4];
        data[0] = (byte) ((startAddress >> 8) & 0xFF);
        data[1] = (byte) (startAddress & 0xFF);
        data[2] = (byte) ((quantity >> 8) & 0xFF);
        data[3] = (byte) (quantity & 0xFF);
        
        return new ModbusRequest(unitId, ModbusFunction.READ_HOLDING_REGISTERS, data);
    }
    
    private int nextTransactionId() {
        return transactionIdGenerator.incrementAndGet() & 0xFFFF;
    }
    
    public static class Builder {
        private String host;
        private int port = 502;
        private int timeoutMs = 3000;
        private RetryPolicy retryPolicy;
        
        public Builder host(String host) {
            this.host = host;
            return this;
        }
        
        public Builder port(int port) {
            this.port = port;
            return this;
        }
        
        public Builder timeout(int timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }
        
        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }
        
        public ModbusMaster build() {
            return new ModbusMaster(this);
        }
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
mvn test -Dtest=ModbusMasterTest -pl Z-Bishop
```
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/master/ModbusMaster.java
git add Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/modbus/master/ModbusMasterTest.java
git commit -m "feat: implement ModbusMaster client with TCP support"
```

---

### Task 7: 实现 ModbusSlave 从站类

**Files:**
- Create: `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/slave/ModbusSlave.java`
- Create: `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/slave/ModbusSlaveConfig.java`
- Test: `Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/modbus/slave/ModbusSlaveTest.java` (新建)

- [ ] **Step 1: 编写 ModbusSlave 测试**

```java
package com.isahl.chess.bishop.protocol.modbus.slave;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModbusSlaveTest {
    
    @Test
    void testBuilder() {
        ModbusSlave slave = ModbusSlave.builder()
            .port(502)
            .unitId(1)
            .build();
        
        assertNotNull(slave);
        assertFalse(slave.isRunning());
    }
    
    @Test
    void testModbusSlaveConfig() {
        ModbusSlaveConfig config = ModbusSlaveConfig.builder()
            .port(502)
            .unitId(1)
            .timeout(3000)
            .build();
        
        assertEquals(502, config.getPort());
        assertEquals(1, config.getUnitId());
        assertEquals(3000, config.getTimeoutMs());
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
mvn test -Dtest=ModbusSlaveTest -pl Z-Bishop
```
Expected: FAIL (类不存在)

- [ ] **Step 3: 实现 ModbusSlave 和 ModbusSlaveConfig 类**

```java
package com.isahl.chess.bishop.protocol.modbus.slave;

/**
 * Modbus 从站配置
 */
public class ModbusSlaveConfig {
    
    private final int port;
    private final int unitId;
    private final int timeoutMs;
    
    private ModbusSlaveConfig(Builder builder) {
        this.port = builder.port;
        this.unitId = builder.unitId;
        this.timeoutMs = builder.timeoutMs;
    }
    
    public int getPort() {
        return port;
    }
    
    public int getUnitId() {
        return unitId;
    }
    
    public int getTimeoutMs() {
        return timeoutMs;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private int port = 502;
        private int unitId = 1;
        private int timeoutMs = 3000;
        
        public Builder port(int port) {
            this.port = port;
            return this;
        }
        
        public Builder unitId(int unitId) {
            this.unitId = unitId;
            return this;
        }
        
        public Builder timeout(int timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }
        
        public ModbusSlaveConfig build() {
            return new ModbusSlaveConfig(this);
        }
    }
}
```

```java
package com.isahl.chess.bishop.protocol.modbus.slave;

/**
 * Modbus 从站 (服务端)
 * Phase 1: 简化实现，仅支持配置
 */
public class ModbusSlave {
    
    private final ModbusSlaveConfig config;
    private boolean running;
    
    private ModbusSlave(ModbusSlaveConfig config) {
        this.config = config;
    }
    
    public static ModbusSlave create(ModbusSlaveConfig config) {
        return new ModbusSlave(config);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public void start() {
        this.running = true;
        // Phase 2: 实现完整的服务端监听和请求处理
    }
    
    public void stop() {
        this.running = false;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public ModbusSlaveConfig getConfig() {
        return config;
    }
    
    public static class Builder {
        private int port = 502;
        private int unitId = 1;
        private int timeoutMs = 3000;
        
        public Builder port(int port) {
            this.port = port;
            return this;
        }
        
        public Builder unitId(int unitId) {
            this.unitId = unitId;
            return this;
        }
        
        public Builder timeout(int timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }
        
        public ModbusSlave build() {
            ModbusSlaveConfig config = ModbusSlaveConfig.builder()
                .port(port)
                .unitId(unitId)
                .timeout(timeoutMs)
                .build();
            return new ModbusSlave(config);
        }
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
mvn test -Dtest=ModbusSlaveTest -pl Z-Bishop
```
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/slave/ModbusSlave.java
git add Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/slave/ModbusSlaveConfig.java
git add Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/modbus/slave/ModbusSlaveTest.java
git commit -m "feat: add ModbusSlave server stub (Phase 2 full implementation)"
```

---

## Chunk 3: 重试策略与标签系统

### Task 8: 实现 RetryPolicy 重试策略接口和 ExponentialBackoffRetry

**Files:**
- Create: `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/retry/RetryPolicy.java`
- Create: `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/retry/ExponentialBackoffRetry.java`
- Test: `Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/modbus/retry/ExponentialBackoffRetryTest.java` (新建)

- [ ] **Step 1: 编写 ExponentialBackoffRetry 测试**

```java
package com.isahl.chess.bishop.protocol.modbus.retry;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExponentialBackoffRetryTest {
    
    @Test
    void testShouldRetry_withinMaxRetries() {
        ExponentialBackoffRetry policy = new ExponentialBackoffRetry(3, 1000);
        
        assertTrue(policy.shouldRetry(0, new RuntimeException()));
        assertTrue(policy.shouldRetry(1, new RuntimeException()));
        assertTrue(policy.shouldRetry(2, new RuntimeException()));
        assertFalse(policy.shouldRetry(3, new RuntimeException()));
    }
    
    @Test
    void testGetDelay_exponentialBackoff() {
        ExponentialBackoffRetry policy = new ExponentialBackoffRetry(5, 1000);
        
        assertEquals(1000, policy.getDelay(0));
        assertEquals(2000, policy.getDelay(1));
        assertEquals(4000, policy.getDelay(2));
    }
    
    @Test
    void testGetDelay_withMaxDelay() {
        ExponentialBackoffRetry policy = new ExponentialBackoffRetry(5, 1000, 5000);
        
        assertEquals(1000, policy.getDelay(0));
        assertEquals(2000, policy.getDelay(1));
        assertEquals(4000, policy.getDelay(2));
        assertEquals(5000, policy.getDelay(3)); // Capped at maxDelay
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
mvn test -Dtest=ExponentialBackoffRetryTest -pl Z-Bishop
```
Expected: FAIL (类不存在)

- [ ] **Step 3: 实现 RetryPolicy 接口和 ExponentialBackoffRetry 类**

```java
package com.isahl.chess.bishop.protocol.modbus.retry;

/**
 * 重试策略接口
 */
public interface RetryPolicy {
    
    /**
     * 是否应该重试
     */
    boolean shouldRetry(int attempt, Throwable exception);
    
    /**
     * 获取重试延迟 (毫秒)
     */
    long getDelay(int attempt);
}
```

```java
package com.isahl.chess.bishop.protocol.modbus.retry;

/**
 * 指数退避重试策略
 */
public class ExponentialBackoffRetry implements RetryPolicy {
    
    private final int maxRetries;
    private final long initialDelayMs;
    private final long maxDelayMs;
    private final double multiplier;
    
    public ExponentialBackoffRetry(int maxRetries, long initialDelayMs) {
        this(maxRetries, initialDelayMs, Long.MAX_VALUE, 2.0);
    }
    
    public ExponentialBackoffRetry(int maxRetries, long initialDelayMs, long maxDelayMs) {
        this(maxRetries, initialDelayMs, maxDelayMs, 2.0);
    }
    
    public ExponentialBackoffRetry(int maxRetries, long initialDelayMs, long maxDelayMs, double multiplier) {
        this.maxRetries = maxRetries;
        this.initialDelayMs = initialDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.multiplier = multiplier;
    }
    
    @Override
    public boolean shouldRetry(int attempt, Throwable exception) {
        return attempt < maxRetries;
    }
    
    @Override
    public long getDelay(int attempt) {
        long delay = (long) (initialDelayMs * Math.pow(multiplier, attempt));
        return Math.min(delay, maxDelayMs);
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
mvn test -Dtest=ExponentialBackoffRetryTest -pl Z-Bishop
```
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/retry/RetryPolicy.java
git add Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/retry/ExponentialBackoffRetry.java
git add Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/modbus/retry/ExponentialBackoffRetryTest.java
git commit -m "feat: implement exponential backoff retry policy"
```

---

### Task 9: 实现标签系统 DataType 和 DataArea 枚举

**Files:**
- Create: `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/tag/DataType.java`
- Create: `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/tag/DataArea.java`
- Create: `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/tag/ByteOrder.java`
- Test: `Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/modbus/tag/DataTypeTest.java` (新建)

- [ ] **Step 1: 编写 DataType 和 DataArea 测试**

```java
package com.isahl.chess.bishop.protocol.modbus.tag;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DataTypeTest {
    
    @Test
    void testDataType_registers() {
        assertEquals(0, DataType.BOOLEAN.getRegisters());
        assertEquals(1, DataType.INT16.getRegisters());
        assertEquals(2, DataType.FLOAT32.getRegisters());
        assertEquals(4, DataType.FLOAT64.getRegisters());
    }
    
    @Test
    void testDataArea() {
        assertEquals(0, DataArea.COIL.getAddressOffset());
        assertEquals(1, DataArea.DISCRETE_INPUT.getAddressOffset());
        assertEquals(3, DataArea.INPUT_REGISTER.getAddressOffset());
        assertEquals(4, DataArea.HOLDING_REGISTER.getAddressOffset());
    }
    
    @Test
    void testByteOrder() {
        assertEquals(ByteOrder.ABCD, ByteOrder.BIG_ENDIAN);
        assertEquals(ByteOrder.CDAB, ByteOrder.LITTLE_ENDIAN);
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
mvn test -Dtest=DataTypeTest -pl Z-Bishop
```
Expected: FAIL (类不存在)

- [ ] **Step 3: 实现枚举类**

```java
package com.isahl.chess.bishop.protocol.modbus.tag;

/**
 * Modbus 数据类型
 */
public enum DataType {
    BOOLEAN(0),         // 1 位 (Coil/Discrete Input)
    INT8(1),            // 8 位有符号
    UINT8(1),           // 8 位无符号
    INT16(1),           // 16 位有符号
    UINT16(1),          // 16 位无符号
    INT32(2),           // 32 位有符号
    UINT32(2),          // 32 位无符号
    INT64(4),           // 64 位有符号
    FLOAT32(2),         // 32 位浮点
    FLOAT64(4),         // 64 位浮点
    STRING(-1);         // 字符串 (可变长度)
    
    private final int registers;
    
    DataType(int registers) {
        this.registers = registers;
    }
    
    public int getRegisters() {
        return registers;
    }
}
```

```java
package com.isahl.chess.bishop.protocol.modbus.tag;

/**
 * Modbus 数据区
 */
public enum DataArea {
    COIL(0),
    DISCRETE_INPUT(1),
    INPUT_REGISTER(3),
    HOLDING_REGISTER(4);
    
    private final int addressOffset;
    
    DataArea(int addressOffset) {
        this.addressOffset = addressOffset;
    }
    
    public int getAddressOffset() {
        return addressOffset;
    }
}
```

```java
package com.isahl.chess.bishop.protocol.modbus.tag;

/**
 * 字节序
 */
public enum ByteOrder {
    ABCD,  // Big Endian
    CDAB,  // Little Endian
    BADC,  // Byte Swap
    DCBA;  // Full Swap
    
    public static final ByteOrder BIG_ENDIAN = ABCD;
    public static final ByteOrder LITTLE_ENDIAN = CDAB;
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
mvn test -Dtest=DataTypeTest -pl Z-Bishop
```
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/tag/DataType.java
git add Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/tag/DataArea.java
git add Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/tag/ByteOrder.java
git add Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/modbus/tag/DataTypeTest.java
git commit -m "feat: add Modbus tag system enums (DataType, DataArea, ByteOrder)"
```

---

### Task 10: 实现 Tag 标签类和 TagManager 标签管理器

**Files:**
- Create: `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/tag/Tag.java`
- Create: `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/tag/TagManager.java`
- Test: `Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/modbus/tag/TagManagerTest.java` (新建)

- [ ] **Step 1: 编写 Tag 和 TagManager 测试**

```java
package com.isahl.chess.bishop.protocol.modbus.tag;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TagManagerTest {
    
    @Test
    void testTagBuilder() {
        Tag tag = Tag.builder()
            .name("Temperature")
            .unitId(1)
            .address(0)
            .dataType(DataType.FLOAT32)
            .dataArea(DataArea.HOLDING_REGISTER)
            .scale(0.1)
            .offset(-40.0)
            .unit("°C")
            .build();
        
        assertEquals("Temperature", tag.getName());
        assertEquals(1, tag.getUnitId());
        assertEquals(0, tag.getAddress());
        assertEquals(DataType.FLOAT32, tag.getDataType());
        assertEquals(0.1, tag.getScale());
    }
    
    @Test
    void testTagManager_registerAndGet() {
        TagManager manager = new TagManager();
        Tag tag = Tag.builder().name("Test").unitId(1).address(0).dataType(DataType.INT16).dataArea(DataArea.HOLDING_REGISTER).build();
        
        manager.register(tag);
        Tag retrieved = manager.getTag("Test");
        
        assertNotNull(retrieved);
        assertEquals("Test", retrieved.getName());
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
mvn test -Dtest=TagManagerTest -pl Z-Bishop
```
Expected: FAIL (类不存在)

- [ ] **Step 3: 实现 Tag 和 TagManager 类**

```java
package com.isahl.chess.bishop.protocol.modbus.tag;

/**
 * Modbus 标签
 */
public class Tag {
    
    private final String name;
    private final String description;
    private final int unitId;
    private final int address;
    private final DataType dataType;
    private final DataArea dataArea;
    private final double scale;
    private final double offset;
    private final String unit;
    private final int precision;
    private final ByteOrder byteOrder;
    
    private Tag(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.unitId = builder.unitId;
        this.address = builder.address;
        this.dataType = builder.dataType;
        this.dataArea = builder.dataArea;
        this.scale = builder.scale;
        this.offset = builder.offset;
        this.unit = builder.unit;
        this.precision = builder.precision;
        this.byteOrder = builder.byteOrder;
    }
    
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getUnitId() { return unitId; }
    public int getAddress() { return address; }
    public DataType getDataType() { return dataType; }
    public DataArea getDataArea() { return dataArea; }
    public double getScale() { return scale; }
    public double getOffset() { return offset; }
    public String getUnit() { return unit; }
    public int getPrecision() { return precision; }
    public ByteOrder getByteOrder() { return byteOrder; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String name;
        private String description;
        private int unitId = 1;
        private int address;
        private DataType dataType = DataType.INT16;
        private DataArea dataArea = DataArea.HOLDING_REGISTER;
        private double scale = 1.0;
        private double offset = 0.0;
        private String unit = "";
        private int precision = 2;
        private ByteOrder byteOrder = ByteOrder.ABCD;
        
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder unitId(int unitId) { this.unitId = unitId; return this; }
        public Builder address(int address) { this.address = address; return this; }
        public Builder dataType(DataType dataType) { this.dataType = dataType; return this; }
        public Builder dataArea(DataArea dataArea) { this.dataArea = dataArea; return this; }
        public Builder scale(double scale) { this.scale = scale; return this; }
        public Builder offset(double offset) { this.offset = offset; return this; }
        public Builder unit(String unit) { this.unit = unit; return this; }
        public Builder precision(int precision) { this.precision = precision; return this; }
        public Builder byteOrder(ByteOrder byteOrder) { this.byteOrder = byteOrder; return this; }
        
        public Tag build() {
            return new Tag(this);
        }
    }
}
```

```java
package com.isahl.chess.bishop.protocol.modbus.tag;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modbus 标签管理器
 */
public class TagManager {
    
    private final Map<String, Tag> tags = new ConcurrentHashMap<>();
    
    public void register(Tag tag) {
        tags.put(tag.getName(), tag);
    }
    
    public Tag getTag(String name) {
        return tags.get(name);
    }
    
    public void unregister(String name) {
        tags.remove(name);
    }
    
    public Map<String, Tag> getAllTags() {
        return Map.copyOf(tags);
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
mvn test -Dtest=TagManagerTest -pl Z-Bishop
```
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/tag/Tag.java
git add Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/tag/TagManager.java
git add Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/modbus/tag/TagManagerTest.java
git commit -m "feat: implement Tag and TagManager for device data modeling"
```

---

## Chunk 4: 监控指标

### Task 11: 实现 ModbusMetrics 监控指标类

**Files:**
- Create: `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/metrics/ModbusMetrics.java`
- Test: `Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/modbus/metrics/ModbusMetricsTest.java` (新建)

- [ ] **Step 1: 编写 ModbusMetrics 测试**

```java
package com.isahl.chess.bishop.protocol.modbus.metrics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModbusMetricsTest {
    
    @Test
    void testRecordRequest() {
        ModbusMetrics metrics = new ModbusMetrics();
        
        metrics.recordRequest(0x03, 100, true);
        
        assertEquals(1, metrics.getRequestCount());
        assertEquals(1, metrics.getSuccessCount());
        assertEquals(0, metrics.getFailureCount());
    }
    
    @Test
    void testRecordError() {
        ModbusMetrics metrics = new ModbusMetrics();
        
        metrics.recordError("timeout");
        
        assertEquals(1, metrics.getErrorCount());
    }
    
    @Test
    void testRecordConnection() {
        ModbusMetrics metrics = new ModbusMetrics();
        
        metrics.recordConnection();
        metrics.recordDisconnection();
        
        assertEquals(1, metrics.getTotalConnections());
        assertEquals(0, metrics.getActiveConnections());
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
mvn test -Dtest=ModbusMetricsTest -pl Z-Bishop
```
Expected: FAIL (类不存在)

- [ ] **Step 3: 实现 ModbusMetrics 类**

```java
package com.isahl.chess.bishop.protocol.modbus.metrics;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Modbus 监控指标
 */
public class ModbusMetrics {
    
    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong totalConnections = new AtomicLong(0);
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final Map<Integer, AtomicLong> functionCodeCounts = new ConcurrentHashMap<>();
    
    public void recordRequest(int functionCode, long durationMs, boolean success) {
        requestCount.incrementAndGet();
        if (success) {
            successCount.incrementAndGet();
        } else {
            failureCount.incrementAndGet();
        }
        
        functionCodeCounts.computeIfAbsent(functionCode, k -> new AtomicLong(0))
                         .incrementAndGet();
    }
    
    public void recordError(String errorType) {
        errorCount.incrementAndGet();
    }
    
    public void recordConnection() {
        totalConnections.incrementAndGet();
        activeConnections.incrementAndGet();
    }
    
    public void recordDisconnection() {
        activeConnections.decrementAndGet();
    }
    
    public long getRequestCount() { return requestCount.get(); }
    public long getSuccessCount() { return successCount.get(); }
    public long getFailureCount() { return failureCount.get(); }
    public long getErrorCount() { return errorCount.get(); }
    public long getTotalConnections() { return totalConnections.get(); }
    public long getActiveConnections() { return activeConnections.get(); }
    
    public Map<Integer, Long> getFunctionCodeDistribution() {
        return functionCodeCounts.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().get()
            ));
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
mvn test -Dtest=ModbusMetricsTest -pl Z-Bishop
```
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/metrics/ModbusMetrics.java
git add Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/modbus/metrics/ModbusMetricsTest.java
git commit -m "feat: add ModbusMetrics for monitoring and observability"
```

---

## Chunk 5: 扩展现有组件

### Task 12: 扩展 ModbusTcpProtocolHandler 支持完整功能码

**Files:**
- Modify: `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/spi/ModbusTcpProtocolHandler.java:96-135`
- Test: `Z-Bishop/src/test/java/com/isahl/chess/audience/bishop/protocol/modbus/ModbusTcpProtocolHandlerTest.java` (已有，扩展)

- [ ] **Step 1: 扩展现有测试**

添加读线圈、写多个寄存器等测试方法。

- [ ] **Step 2: 添加 createReadCoils 和 createWriteMultipleRegisters 方法**

```java
public ModbusMessage createReadCoils(int unitId, int startAddress, int quantity) {
    byte[] data = new byte[4];
    data[0] = (byte) ((startAddress >> 8) & 0xFF);
    data[1] = (byte) (startAddress & 0xFF);
    data[2] = (byte) ((quantity >> 8) & 0xFF);
    data[3] = (byte) (quantity & 0xFF);
    
    return new ModbusMessage(unitId, ModbusFunction.READ_COILS, data);
}

public ModbusMessage createWriteMultipleRegisters(int unitId, int startAddress, int[] values) {
    byte[] data = new byte[5 + values.length * 2];
    data[0] = (byte) ((startAddress >> 8) & 0xFF);
    data[1] = (byte) (startAddress & 0xFF);
    data[2] = (byte) ((values.length >> 8) & 0xFF);
    data[3] = (byte) (values.length & 0xFF);
    data[4] = (byte) (values.length * 2);
    
    for (int i = 0; i < values.length; i++) {
        data[5 + i * 2] = (byte) ((values[i] >> 8) & 0xFF);
        data[5 + i * 2 + 1] = (byte) (values[i] & 0xFF);
    }
    
    return new ModbusMessage(unitId, ModbusFunction.WRITE_MULTIPLE_REGISTERS, data);
}
```

- [ ] **Step 3: 运行测试验证通过**

```bash
mvn test -Dtest=ModbusTcpProtocolHandlerTest -pl Z-Bishop
```
Expected: PASS

- [ ] **Step 4: 提交**

```bash
git add Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/spi/ModbusTcpProtocolHandler.java
git add Z-Audience/src/test/java/com/isahl/chess/audience/bishop/protocol/modbus/ModbusTcpProtocolHandlerTest.java
git commit -m "feat: extend ModbusTcpProtocolHandler with additional function codes"
```

---

### Task 13: 创建 ModbusConstants 常量类

**Files:**
- Create: `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/ModbusConstants.java`

- [ ] **Step 1: 实现 ModbusConstants 类**

```java
package com.isahl.chess.bishop.protocol.modbus;

/**
 * Modbus 协议常量
 */
public final class ModbusConstants {
    
    private ModbusConstants() {}
    
    // 默认端口
    public static final int DEFAULT_TCP_PORT = 502;
    public static final int DEFAULT_TLS_PORT = 802;
    
    // MBAP 报文头长度
    public static final int MBAP_HEADER_LENGTH = 7;
    
    // RTU 帧最小长度
    public static final int RTU_MIN_FRAME_LENGTH = 4;
    
    // 最大 PDU 长度
    public static final int MAX_PDU_LENGTH = 253;
    
    // 广播地址
    public static final int BROADCAST_UNIT_ID = 0;
    
    // 最大从站地址
    public static final int MAX_UNIT_ID = 247;
    
    // 事务 ID 最大值
    public static final int MAX_TRANSACTION_ID = 65535;
    
    // 3.5 字符静默间隔 (毫秒，9600 波特率)
    public static final int RTU_SILENT_INTERVAL_MS = 4; // 3.5 * 11 * 1000 / 9600 ≈ 4ms
}
```

- [ ] **Step 2: 提交**

```bash
git add Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/ModbusConstants.java
git commit -m "feat: add ModbusConstants for protocol constants"
```

---

## Chunk 6: 集成测试

### Task 14: 创建 ModbusIntegrationTest 集成测试

**Files:**
- Create: `Z-Audience/src/test/java/com/isahl/chess/audience/bishop/protocol/modbus/ModbusIntegrationTest.java`

- [ ] **Step 1: 编写集成测试**

```java
package com.isahl.chess.audience.bishop.protocol.modbus;

import com.isahl.chess.bishop.protocol.modbus.master.ModbusMaster;
import com.isahl.chess.bishop.protocol.modbus.master.ModbusResponse;
import com.isahl.chess.bishop.protocol.modbus.retry.ExponentialBackoffRetry;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Modbus 集成测试
 * 
 * 注意：此测试需要真实的 Modbus 从站设备或模拟器
 * 运行前请确保启动 Modbus 从站模拟器在 localhost:502
 */
@Disabled("Requires Modbus slave simulator")
class ModbusIntegrationTest {
    
    @Test
    void testMasterReadHoldingRegisters() throws Exception {
        ModbusMaster master = ModbusMaster.builder()
            .host("localhost")
            .port(502)
            .timeout(3000)
            .retryPolicy(new ExponentialBackoffRetry(3, 1000))
            .build();
        
        master.connect();
        
        ModbusResponse response = master.sendRequest(
            master.createReadHoldingRegisters(1, 0, 10)
        );
        
        assertNotNull(response);
        assertEquals(1, response.getUnitId());
        
        master.close();
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
mvn test -Dtest=ModbusIntegrationTest -pl Z-Audience
```
Expected: SKIP (被@Disabled 标注)

- [ ] **Step 3: 提交**

```bash
git add Z-Audience/src/test/java/com/isahl/chess/audience/bishop/protocol/modbus/ModbusIntegrationTest.java
git commit -m "test: add Modbus integration test (requires slave simulator)"
```

---

## 计划完成

计划已完整编写。所有任务遵循 TDD 流程：
1. 先写失败测试
2. 验证测试失败
3. 写最小实现
4. 验证测试通过
5. 提交

**下一步**: 使用 `superpowers:subagent-driven-development` 执行此计划。
