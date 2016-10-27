/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

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
                switch (message.getFunctionCode()) {
                    case FwUtil.FP_CODE_64H:
                        break;
                    case FwUtil.FP_CODE_10H:
                        break;
                    case FwUtil.FP_CODE_34H:
                        break;
                    case FwUtil.FP_CODE_35H:
                        break;
                    case FwUtil.FP_CODE_36H:
                        break;
                    case FwUtil.FP_CODE_30H:
                        break;
                    case FwUtil.FP_CODE_INFO:
                        break;
                    case FwUtil.FP_CODE_91H:
                        break;

                    default:
                        return;

                }
                len = message.toBuffer(FwUtil.outbuf, 5);
                FwUtil.IntToBuff(FwUtil.outbuf, 0, len);
                int crc = FwUtil.Crc(FwUtil.outbuf, 0, len + 5);
                FwUtil.IntToBuff(FwUtil.outbuf, 5 + len, crc);
                size = len + 7;
                m_Output.write(FwUtil.outbuf, 0, size);
            }
            catch (IOException ex) {
                if (FwUtil.FP_DEBUG) {
                    System.err.println("FwMessage " + ex.getMessage());
                }
            }
        }
    }
}
