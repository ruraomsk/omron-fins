/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

import java.io.IOException;
import java.net.Socket;

/**
 * 
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
class FwSlaveListner extends Thread
{

    private FwSlaveDevice mydevice = null;
    private FwTransport myTransport = null;
    private boolean ready = false;
    private String name;
    private Integer port;
    private Socket socket;
    private long stepTime;
    private int error = 0;
    private SlaveListner mySlave = null;

    FwSlaveListner(String name, Integer port, Socket insc)
    {
        // узнаем свое устройство
        this.name = name;
        this.port = port;
        socket = insc;
        registration();
    }

    private void registration()
    {
        mydevice = FwUtil.S_DEV.get(port);
        if (mydevice == null)
        {
            //System.out.println(name + " not exist device on port:" + port.toString());
            return;
        }
        mySlave = mydevice.appendListner(name, this);
        stepTime = mydevice.getStepTime();
        myTransport = new FwTransport(socket, mydevice.getTableDecode());
        ready = true;
    }

    public String getMyName()
    {
        return name;
    }

    public boolean startListner()
    {
        if (!ready)
        {
            return false;
        }
        if (!myTransport.connect())
        {
            mySlave.disconnect();
            return false;
        }
        start();
        return true;
    }

    @Override
    public void run()
    {
        FwMessage message = null;
        FwResponse resp=null;
        while (!Thread.interrupted())
        {
            try
            {
                    while ((message = mySlave.getMessage()) != null)
                    {
                        //System.out.println("Lister transfered");
                        myTransport.writeMessage(message);
                    }
                    error=0;
                resp=myTransport.readMessage();
                while (resp!=null){
                    //System.out.println("Add response");
                    mySlave.addResponse(resp);
                    resp=myTransport.readMessage();
                }
                Thread.sleep(stepTime);
            }
            catch (InterruptedException ex)
            {
                if(FwUtil.FP_DEBUG) System.err.println("FwSlaveListner "+ex.getMessage());
                mySlave.disconnect();
                myTransport.close();
            }
        }
    }

}
