/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

import java.io.IOException;
import java.util.Arrays;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class FwResponse
{

    private int controller;
    private byte functionCode;
    private Object info = null;

    FwResponse(int controller, byte functionCode, int len, byte[] buffer, FwRegisters tableDecode)
    {
        this.controller = controller;
        this.functionCode = functionCode;


        switch (functionCode) {
            case FwUtil.FP_CODE_INFO:
                info = new FwInfo(len, buffer, controller, tableDecode);
                break;
            case FwUtil.FP_CODE_35H:
                
                info = new FwKvit(buffer, controller);
//                System.err.println("FwResponse Kvit controller="+Integer.toString(controller));
                break;
            case FwUtil.FP_CODE_34H:
//              byte[] b=new byte[len];
//              System.arraycopy(buffer, 0, b, 0, len);
//              System.err.println("FwResponse"+Arrays.toString(b));
                info = new FwSetup(true, len, buffer, controller);
                break;
            case FwUtil.FP_CODE_36H:
                info = new FwSetup(false, len, buffer, controller);
                break;
            case FwUtil.FP_CODE_30H:
                info = new FwDiag(len, buffer, controller, tableDecode);
                break;
            case FwUtil.FP_CODE_64H:
                info = new FwMesLive(buffer, controller);
                break;
            case FwUtil.FP_CODE_91H:
                info = new FwSyncTime(buffer, controller);
                break;
            case FwUtil.FP_CODE_10H:
                info = new FwMesCtrl(buffer, controller);
                break;
            default: {
                if (FwUtil.FP_DEBUG) {
                    System.err.println("Bad function code.");
                }
                //throw new IOException("Bad function code."+Integer.toString(this.getFunctionCode()));
            }
        }
    }

    /**
     * @return the controller
     */
    public int getController()
    {
        return controller;
    }

    /**
     * @return the functionCode
     */
    public byte getFunctionCode()
    {
        return functionCode;
    }

    /**
     * @return the info
     */
    public FwInfo getInfo()
    {
        if (getFunctionCode() == FwUtil.FP_CODE_INFO) {
            return (FwInfo) info;
        }
        return null;
    }

    public FwSetup getSetup()
    {
        if (getFunctionCode() == FwUtil.FP_CODE_34H
                || getFunctionCode() == FwUtil.FP_CODE_36H) {
            return (FwSetup) info;
        }
        return null;
    }

    /**
     * @return the kvit
     */
    public FwKvit getKvit()
    {
        if (getFunctionCode() == FwUtil.FP_CODE_35H) {
            return (FwKvit) info;
        }
        return null;
    }

    /**
     * @return the synctime
     */
    public FwSyncTime getSynctime()
    {
        if (getFunctionCode() == FwUtil.FP_CODE_91H) {
            return (FwSyncTime) info;
        }
        return null;
    }

    /**
     *
     * @return the MesLive
     */
    public FwMesLive getMesLive()
    {
        if (getFunctionCode() == FwUtil.FP_CODE_64H) {
            return (FwMesLive) info;
        }
        return null;
    }

    public FwMesCtrl getMesCtrl()
    {
        if (getFunctionCode() == FwUtil.FP_CODE_10H) {
            return (FwMesCtrl) info;
        }
        return null;
    }

    /**
     * @return the diag
     */
    public FwDiag getDiag()
    {
        if (getFunctionCode() == FwUtil.FP_CODE_30H) {
            return (FwDiag) info;
        }
        return null;
    }
}
