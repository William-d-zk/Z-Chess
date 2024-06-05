package com.isahl.chess.player.api.component;

import com.isahl.chess.king.base.cron.features.ICancelable;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.pawn.endpoint.device.db.central.model.MessageEntity;
import com.isahl.chess.pawn.endpoint.device.spi.IHandleHook;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author xiaojiang.lxj at 2024-06-05 17:05.
 */
@Component
public class BusinessPlugin implements IHandleHook, ICancelable {

    private final List<IBusinessSubscribe> _IBusinessSubscribes;

    public BusinessPlugin(
        List<IBusinessSubscribe> iBusinessSubscribes) {
        _IBusinessSubscribes = iBusinessSubscribes;
    }


    @Override
    public void cancel() {

    }

    @Override
    public void afterLogic(IoSerial content, List<ITriple> results) {

    }

    @Override
    public void afterConsume(IoSerial content) {
        if(content instanceof MessageEntity msg){
            for(IBusinessSubscribe iBusinessSubscribe : _IBusinessSubscribes){
                iBusinessSubscribe.onMessage(msg);
            }
        }
    }

    @Override
    public boolean isExpect(IoSerial content) {
        return content.serial() == 0x113 || content.serial() == 0x1D || content instanceof MessageEntity;
    }

    public interface IBusinessSubscribe
    {
        void onMessage(IoSerial content);
    }
}
