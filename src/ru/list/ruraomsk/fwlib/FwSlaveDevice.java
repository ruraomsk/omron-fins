/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class FwSlaveDevice extends Thread
{

    public ConcurrentHashMap<String, SlaveListner> listners = null;
    private ConcurrentHashMap<Long, FwOneReg> data = null;
    private int port;
    private int controller;
    public int nomerinfo = 1;
    public int nomerdiag = 0;

    private FwInfo info;
    private FwRegisters tableDecode = null;
    private final long stepTime = FwUtil.FP_STEP_TIME;
    public int error = 0;
    public Date lastsync = new Date();
    public Date lastdiag = new Date();

    public FwSlaveDevice(int port, int controller, FwRegisters tableDecode)
    {
        this.port = port;
        this.controller = controller;
        this.tableDecode = tableDecode;
        // регистрируем устройство 
        listners = new ConcurrentHashMap<>();
        data = new ConcurrentHashMap<>(FwUtil.VALUE_UIDS);
        FwUtil.S_DEV.put(port, this);
        for (FwRegister reg : tableDecode.getCollection())
        {
            if ((reg.getController() == controller))
            {
                FwOneReg onereg = new FwOneReg(new Date(), reg);
                data.put(reg.getKey(), onereg);
            }
        }
        info = new FwInfo(controller, nomerinfo);
        changeAllDate();
        loadAllValues();
        start();
    }

    @Override
    public void run()
    {
        FwResponse resp = null;
        while (!Thread.interrupted())
        {
            try
            {
                for (SlaveListner sl : listners.values())
                {
                    sl.notLive();
                    while ((resp = sl.getResponse()) != null)
                    {
                        if (resp.getController() != getController())
                        {
                            error++;
                            continue;
                        }
                        switch (resp.getFunctionCode())
                        {
                            case FwUtil.FP_CODE_10H:
                                doCtrl(resp.getMesCtrl());
                                break;
                            case FwUtil.FP_CODE_34H:
                                // принять настроечные данные
                                doSetup(resp.getOtvet());
                                break;
                            case FwUtil.FP_CODE_64H:
                                // master is live))
                                sl.isLive();
                                break;
                        }
                    }
                }
                Date dd = new Date();
                if ((dd.getTime() - lastdiag.getTime()) > FwUtil.FP_STEP_DIAG)
                {
                    sendDiag();
                    lastdiag = dd;
                }
                Thread.sleep(stepTime);
            }
            catch (InterruptedException ex)
            {
                if (FwUtil.FP_DEBUG)
                {
                    System.err.println("FwSlaveDevice ошибка " + ex.getMessage());
                }
                //myTransport.close();
            }
        }

    }

    /**
     * Регистрирует нового листнера
     *
     * @param name
     * @param listner
     * @return объект стушателя
     */
    public SlaveListner appendListner(String name, FwSlaveListner listner)
    {
        SlaveListner temp = new SlaveListner(listner);
        listners.put(name, temp);
//        System.err.println("Добавлен "+name);
        return temp;
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

    public void setCode(int uId, Object code)
    {
        long key = FwRegister.makeKey(controller, uId, false);
        FwOneReg t = data.get(key);
        if (t == null)
        {
            return;
        }
        t.setDate(new Date());
        t.setValue(code);
        t.setGood(FwUtil.FP_DATA_GOOD);
        data.put(key, t);
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

    public void setValue(int uId, Object value, byte good)
    {
        long key = FwRegister.makeKey(controller, uId, true);
        FwOneReg temp = data.get(key);

        temp.setDate(new Date());
        temp.setValue(value);
        temp.setGood(good);
        data.put(key, temp);
        appendToInfo(new FwOneReg(temp.getDate(), temp.getReg(), temp.getValue(), temp.getGood()));
    }

    public void setValue(int uId, Object value)
    {
        long key = FwRegister.makeKey(controller, uId, true);
        FwOneReg temp = data.get(key);
        temp.setDate(new Date());
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
        for (FwOneReg oreg : data.values())
        {
            if (oreg.getReg().isInfo())
            {
                appendToInfo(oreg);
            }
        }
    }

    private void appendToInfo(FwOneReg oreg)
    {
        if (info.isFull())
        {
            sendAll(info);
            info = new FwInfo(getController(), nomerinfo++);
            changeAllDate();
            loadAllValues();

        }
        info.addOneReg(oreg);
    }

    public void sendDiag()
    {
        FwDiag mesd = new FwDiag(controller, tableDecode);
        for (FwOneReg dreg : data.values())
        {
            if (dreg.getReg().isDiag())
            {
                mesd.setOneDiag(dreg.getuId(), (int) dreg.getValue());
            }
        }
        nomerdiag++;
        sendAll(mesd);
    }

    private void sendAll(FwBaseMess message)
    {
        //System.out.println("Storage all...");
        for (SlaveListner sl : listners.values())
        {
            if (sl.isconnected())
            {
                sl.addMessage(new FwMessage(message));
                //System.out.println("Send full info message...");
                error = 0;
            }
        }
    }

    private void changeAllDate()
    {
        Date date = new Date();

        for (FwOneReg oreg : data.values())
        {
            oreg.setDate(date);
        }
    }

    private void doCtrl(FwMesCtrl message)
    {
        if (message.getCommandcode() == FwUtil.FP_CTRL_ALLINFO)
        {
            if (info.getSize() != 0)
            {
                sendAll(info);
                info = new FwInfo(getController(), nomerinfo++);
            }
            changeAllDate();
            loadAllValues();
            sendAll(info);
            info = new FwInfo(getController(), nomerinfo++);
        }
        if (message.getCommandcode() == FwUtil.FP_CTRL_TESTSYNC)
        {
            Date now = new Date();
            FwSyncTime temp = new FwSyncTime(getController(), lastsync, now);
            lastsync = now;
            sendAll(temp);
        }
    }

    private void doSetup(FwSetup otvet)
    {

        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * @return the controller
     */
    public int getController()
    {
        return controller;
    }

}
