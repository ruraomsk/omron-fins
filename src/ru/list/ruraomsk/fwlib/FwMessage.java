/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

import com.tibbo.aggregate.common.Log;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
class FwMessage
{

    private FwBaseMess message = null;

    public FwMessage(FwBaseMess message)
    {
        this.message = message;
    }

    public void writeTo(DataOutputStream m_Output)
    {
        synchronized (FwUtil.outbuf) {
            try {
                int size;
                int len = 0;
                FwUtil.IntToBuff(FwUtil.outbuf, 2, message.getController());
                FwUtil.outbuf[4] = message.getFunctionCode();
                len = message.toBuffer(FwUtil.outbuf, 5);
                FwUtil.IntToBuff(FwUtil.outbuf, 0, len);
                int crc = FwUtil.Crc(FwUtil.outbuf, 0, len + 5);
                FwUtil.IntToBuff(FwUtil.outbuf, 5 + len, crc);
                size = len + 7;
                m_Output.write(FwUtil.outbuf, 0, size);
            }
            catch (IOException ex) {
                    Log.CORE.error("FwMessage " + ex.getMessage());
            }
        }
    }
}
