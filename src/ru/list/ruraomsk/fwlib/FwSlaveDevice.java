/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

import com.tibbo.aggregate.common.Log;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class FwSlaveDevice extends Thread
{

//    public ConcurrentHashMap<String, SlaveListner> listners = null;
    private ConcurrentHashMap<Integer, FwOneReg> data = null;

    private ConcurrentLinkedQueue<FwBaseMess> inmessages;
    private ConcurrentLinkedQueue<FwBaseMess> outmessages;

    private Socket socket;
    private int controller;
    public int nomerinfo = 1;
    public int nomerdiag = 0;

    private FwInfo info;
    private FwRegisters tableDecode = null;
    private final long stepTime = FwUtil.FP_STEP_TIME;
    private int count=0;
    public int error = 0;
    public Date lastsync = new Date();
    public Date lastdiag = new Date();
    private String UPCMessage = "";
    private FwTransport myTransport;
    public FwSlaveDevice(Socket socket, int controller, FwRegisters tableDecode)
    {
        this.inmessages = new ConcurrentLinkedQueue<>();
        this.outmessages = new ConcurrentLinkedQueue<>();
        this.socket = socket;
        this.controller = controller;
        this.tableDecode = tableDecode;
        // регистрируем устройство 
        data = new ConcurrentHashMap<>(FwUtil.VALUE_UIDS);
        for (FwRegister reg : tableDecode.getCollection()) {
            if ((reg.getController() == controller)) {
                FwOneReg onereg = new FwOneReg(System.currentTimeMillis(), reg);
                data.put(reg.getKey(), onereg);
            }
        }
        myTransport = new FwTransport(socket, this.tableDecode);

        info = new FwInfo(controller, nomerinfo);
        changeAllDate();
        start();
    }

    @Override
    public void run()
    {
        FwResponse resp = null;
        while (!Thread.interrupted()) {
            try {
                    notLive();
                    while ((resp = myTransport.readMessage()) != null) {
                        if (resp.getController() != getController()) {
                            error++;
                            continue;
                        }
                        switch (resp.getFunctionCode()) {
                            case FwUtil.FP_CODE_10H:
                                outmessages.add(resp.getMesCtrl());
                                doCtrl(resp.getMesCtrl());
                                break;
                            case FwUtil.FP_CODE_34H:
                            case FwUtil.FP_CODE_36H:
                                // принять настроечные данные
                                outmessages.add(resp.getSetup());
                                break;
                            case FwUtil.FP_CODE_35H:
                                // принять команды
                                outmessages.add(resp.getKvit());
                                break;
                            case FwUtil.FP_CODE_64H:
                                // master is live))
                                break;
                        }

                }
                FwBaseMess message;
                while ((message = inmessages.poll()) != null) {
                        myTransport.writeMessage(new FwMessage(message));
                    }

                Date dd = new Date();
                if ((dd.getTime() - lastdiag.getTime()) > FwUtil.FP_STEP_DIAG) {
                    sendDiag();
                    lastdiag = dd;
                }
                Thread.sleep(stepTime);
            }
            catch (InterruptedException ex) {
                    Log.CORE.error("FwSlaveDevice ошибка " + ex.getMessage());
            }
        }

    }

    public void notLive()
    {
        if (++count > 32000) {
            count = 32000;
        }
    }

    public void isLive()
    {
        count = 0;
    }

    public Object getCode(int uId)
    {
        int key = FwRegister.makeKey(controller, uId, false);
        if (data.get(key) == null) {
            return null;
        }
        return data.get(key).getValue();
    }

    public void setCode(int uId, Object code)
    {
        int key = FwRegister.makeKey(controller, uId, false);
        FwOneReg t = data.get(key);
        if (t == null) {
            return;
        }
        t.setDate(System.currentTimeMillis());
        t.setValue(code);
        t.setGood(FwUtil.FP_DATA_GOOD);
        data.put(key, t);
    }

    public Object getValue(int uId)
    {
        int key = FwRegister.makeKey(controller, uId, true);
        if (data.get(key) == null) {
            return null;
        }
        return data.get(key).getValue();
    }

    public void setValue(int uId, Object value, byte good)
    {
        int key = FwRegister.makeKey(controller, uId, true);
        FwOneReg temp = data.get(key);

        temp.setDate(System.currentTimeMillis());
        temp.setValue(value);
        temp.setGood(good);
        data.put(key, temp);
        appendToInfo(new FwOneReg(temp.getDate(), temp.getReg(), temp.getValue(), temp.getGood()));
    }

    public void setValue(int uId, Object value)
    {
        int key = FwRegister.makeKey(controller, uId, true);
        FwOneReg temp = data.get(key);
        temp.setDate(System.currentTimeMillis());
        temp.setValue(value);
        temp.setGood(FwUtil.FP_DATA_GOOD);
        data.put(key, temp);
        appendToInfo(new FwOneReg(temp.getDate(), temp.getReg(), temp.getValue(), temp.getGood()));
    }

    public FwRegisters getTableDecode()
    {
        return tableDecode;
    }

    public long getStepTime()
    {
        return stepTime;
    }

    private void loadAllValues()
    {
        for (FwOneReg oreg : data.values()) {
            if (oreg.getReg().isInfo()) {
                appendToInfo(oreg);
            }
        }
    }

    private void appendToInfo(FwOneReg oreg)
    {
        if (info.isFull()) {
            sendAll(info);
            info = new FwInfo(getController(), nomerinfo++);
            changeAllDate();

        }
        info.addOneReg(oreg);
    }

    public void sendDiag()
    {
        FwDiag mesd = new FwDiag(controller, tableDecode);
        for (FwOneReg dreg : data.values()) {
            if (dreg.getReg().isDiag()) {
                mesd.setOneDiag(dreg.getuId(), (int) dreg.getValue());
            }
        }
        mesd.setUPCMessage(UPCMessage);
        nomerdiag++;
        sendAll(mesd);
    }

    private void sendAll(FwBaseMess message)
    {
                myTransport.writeMessage(new FwMessage(message));
                error = 0;
    }

    private void changeAllDate()
    {
        long date = System.currentTimeMillis();

        for (FwOneReg oreg : data.values()) {
            oreg.setDate(date);
            oreg.setGood(FwUtil.FP_DATA_GOOD);
        }
    }

    private void doCtrl(FwMesCtrl message)
    {
        if (message.getCommandcode() == FwUtil.FP_CTRL_ALLINFO) {
            if (info.getSize() != 0) {
                sendAll(info);
                info = new FwInfo(getController(), nomerinfo++);
            }
            changeAllDate();
            loadAllValues();
            sendAll(info);
            info = new FwInfo(getController(), nomerinfo++);
        }
        if (message.getCommandcode() == FwUtil.FP_CTRL_TESTSYNC) {
            Date now = new Date();
            FwSyncTime temp = new FwSyncTime(getController(), lastsync, now);
            lastsync = now;
            sendAll(temp);
        }
    }

    /**
     * @return the controller
     */
    public int getController()
    {
        return controller;
    }

    public void setUPCMessage(String UPCMessage)
    {
        this.UPCMessage = UPCMessage;
    }

    public void putMessage(FwBaseMess message)
    {
        inmessages.add(message);
    }

    public FwBaseMess readMessage()
    {
        return outmessages.poll();
    }

    public boolean isconnected() {
        return myTransport.isConnected();
    }

    public int LiveCount() {
        return count;
    }

}
