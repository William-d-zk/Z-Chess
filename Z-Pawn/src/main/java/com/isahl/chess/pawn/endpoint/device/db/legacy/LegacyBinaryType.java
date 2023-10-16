package com.isahl.chess.pawn.endpoint.device.db.legacy;

import org.hibernate.usertype.UserTypeLegacyBridge;

/**
 * 升级hibernate-core 6.x, 兼容UserType变更
 *
 * @author xiaojiang.lxj at 2023-10-13 16:43.
 */
public class LegacyBinaryType extends UserTypeLegacyBridge {

    public LegacyBinaryType() {
        super("org.hibernate.type.BinaryType");
    }
}
