/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class FwMasterDevice extends Thread
{

    private ConcurrentHashMap<Long, FwOneReg> data = null;
    private ConcurrentLinkedQueue<FwOneReg> history = null;
    private ConcurrentLinkedQueue<FwBaseMess> inmessages = null;
    private Socket socket;
    private int controller;
    public int nomerinfo = 1;
    private FwInfo info = null;
    private FwRegisters tableDecode = null;
    private long stepTime = FwUtil.FP_STEP_TIME;
    public int error = -1;
    public Date lastsync = new Date();
    private Date lastlive = new Date();
    private boolean connect = false;
    public FwTransport mytransport = null;
    private int countlive = 0;
    private long stepGiveMe = FwUtil.FP_STEP_GIVEME;
    private long stepLive = FwUtil.FP_STEP_LIVE;
    private byte errFuncCode = 0;
    private long lastGiveMe = new Date().getTime();
    private long lastLive = new Date().getTime();

    public FwMasterDevice(Socket socket, int controller, FwRegisters tableDecode)
    {
        this.socket = socket;
        this.controller = controller;
        this.tableDecode = tableDecode;
        data = new ConcurrentHashMap<>(FwUtil.VALUE_UIDS);
        history = new ConcurrentLinkedQueue<>();
        inmessages = new ConcurrentLinkedQueue<>();
        for (FwRegister reg : tableDecode.getCollection())
        {
            if ((reg.getController() == controller))
            {
                FwOneReg onereg = new FwOneReg(new Date(), reg);
                data.put(reg.getKey(), onereg);
            }
        }
        for (FwOneReg oreg : data.values())
        {
            history.add(oreg);
        }
    }


    public void gogo()
    {
        try
        {
            connect=false;
            mytransport = new FwTransport(socket, tableDecode);
            if (mytransport == null)
            {
                error = 5;
                return;
            }
            if (!(connect = mytransport.connect()))
            {
                error = 6;
                return;
            }
        }
        catch (Exception ex)
        {
            error = 12;
            return;
        }
        if (!connect)
        {
            return;
        }
        start();

    }

    public boolean isConnected()
    {
        return connect;
    }

    public int getErrors()
    {
        return error;
    }

    public void disconect()
    {
        mytransport.close();
        connect = false;
    }

    public String myAddress()
    {
        return socket.getInetAddress().getHostAddress() + ":" + Integer.toString(socket.getPort());
    }

    @Override
    public void run()
    {
        FwMesCtrl givemeAll = new FwMesCtrl(controller, FwUtil.FP_CTRL_ALLINFO);
        sendMessage(givemeAll);
        lastGiveMe = new Date().getTime();
        FwResponse resp = null;
        int count = 1;
        error = 0;
        try
        {
            while (!Thread.interrupted() & connect)
            {
                long now = new Date().getTime();
                if ((now - lastGiveMe) > getStepGiveMe())
                {
                    givemeAll = new FwMesCtrl(controller, FwUtil.FP_CTRL_ALLINFO);
                    sendMessage(givemeAll);
                    lastGiveMe = now;
                }
                if ((now - lastLive) > getStepLive())
                {
                    FwMesLive meslive = new FwMesLive(controller, count);
                    sendMessage(meslive);
                    lastLive = now;
                    count++;
                }

                //System.out.println("MasterDev in");
                resp = mytransport.readMessage();
                while (resp != null)
                {
                    //if(FwUtil.FP_DEBUG) System.err.println("step 1");
                    if (resp.getController() != controller)
                    {
                        error = 1;
                        errFuncCode = resp.getFunctionCode();
                        disconect();
                        break;
                    }
                    //if(FwUtil.FP_DEBUG) System.err.println("step 2");
                    switch (resp.getFunctionCode())
                    {
                        case FwUtil.FP_CODE_INFO:
                            readValues(resp.getInfo());
                            break;
                        case FwUtil.FP_CODE_35H:
                            // kvit
                            break;
                        case FwUtil.FP_CODE_36H:
                            break;
                        case FwUtil.FP_CODE_30H:
                            readDiags(resp.getDiag());
                            break;
                        case FwUtil.FP_CODE_91H:
                            break;
                    }
                    resp = mytransport.readMessage();
                }
                Thread.sleep(stepTime);
                connect = mytransport.isConnected();
            }

        }
        catch (InterruptedException ex)
        {
            if (FwUtil.FP_DEBUG)
            {
                System.err.println("FwMasterDevice " + ex.getMessage());
            }
            error = 3;
            disconect();
        }
    }

    private void readValues(FwInfo info)
    {
        nomerinfo = info.getNomer();
        for (int i = 0; i < info.getSize(); i++)
        {
            FwOneReg value = info.getOneReg(i);
            //System.out.println("===uId="+Integer.toString(value.getuId())+" value="+value.getValue().toString());
            history.add(value);

            data.put(value.getReg().getKey(), value);
        }
    }

    public FwOneReg getOneReg(int uId)
    {
        long key = FwRegister.makeKey(controller, uId, true);

        return data.get(key);
    }

    public FwOneReg getHistory()
    {
        return history.poll();
    }

    public FwRegisters getTableDecode()
    {
        return tableDecode;
    }

    public int getController()
    {
        return controller;
    }

    public long getStepTime()
    {
        return stepTime;
    }

    public void setStepTime(long newStep)
    {
        stepTime = newStep;
    }

    public void setStepGiveMe(long newStep)
    {
        stepGiveMe = newStep;
    }

    public void setStepLive(long newStep)
    {
        stepLive = newStep;
    }

    private void sendMessage(FwBaseMess message)
    {
        mytransport.writeMessage(new FwMessage(message));
    }

    public Object getValue(int uId)
    {
        long key = FwRegister.makeKey(controller, uId, true);
        if (data.get(key) == null)
        {
            return null;
        }
        return data.get(key).getValue();
    }

    public Object getCode(int uId)
    {
        long key = FwRegister.makeKey(controller, uId, false);
        if (data.get(key) == null)
        {
            return null;
        }
        return data.get(key).getValue();
    }

    private void readDiags(FwDiag diag)
    {
        for (int i = 0; i < diag.getSize(); i++)
        {
            FwOneReg value = new FwOneReg(new Date(), tableDecode.getRegisterDiag(controller, diag.getDiagUId(i)));
            value.setValue(diag.getDiagCode(i));
            value.setGood(FwUtil.FP_DATA_GOOD);
            history.add(value);

            data.put(value.getReg().getKey(), value);

            //System.out.println("===uId="+Integer.toString(value.getuId())+" value="+value.getValue().toString());
        }
    }

    /**
     * @return the stepGiveMe
     */
    public long getStepGiveMe()
    {
        return stepGiveMe;
    }

    /**
     * @return the stepLive
     */
    public long getStepLive()
    {
        return stepLive;
    }

    /**
     * @return the errFuncCode
     */
    public byte getErrFuncCode()
    {
        return errFuncCode;
    }
}
