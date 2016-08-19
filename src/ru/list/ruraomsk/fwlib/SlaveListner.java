/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
 public class SlaveListner
{

    private ConcurrentLinkedQueue<FwMessage> qMess = new ConcurrentLinkedQueue<FwMessage>();
    private ConcurrentLinkedQueue<FwResponse> qResp = new ConcurrentLinkedQueue<FwResponse>();
    private FwSlaveListner listner = null;
    private boolean connect = true;
    private int count = 0; //счетчик сколько циклов не было сообшения о живучести сервера 

    public SlaveListner(FwSlaveListner listner)
    {
        this.listner = listner;
    }

    public void disconnect()
    {
        connect = false;
    }

    public boolean isconnected()
    {
        return connect;
    }

    public void addMessage(FwMessage message)
    {
        qMess.add(message);
    }

    public FwMessage getMessage()
    {
        return qMess.poll();
    }

    public void addResponse(FwResponse message)
    {
        qResp.add(message);
    }

    public FwResponse getResponse()
    {
        return qResp.poll();
    }

    public String getName()
    {
        return listner.getMyName();
    }

    public void notLive()
    {
        if (++count > 32000)
        {
            count = 32000;
        }
    }

    public void isLive()
    {
        count = 0;
    }

    public int LiveCount()
    {
        return count;
    }
}
