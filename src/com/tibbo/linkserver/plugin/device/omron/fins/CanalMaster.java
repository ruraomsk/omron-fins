/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tibbo.linkserver.plugin.device.omron.fins;

import com.tibbo.aggregate.common.Log;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import ru.list.ruraomsk.fwlib.FwMasterDevice;
import ru.list.ruraomsk.fwlib.FwOneReg;
import ru.list.ruraomsk.fwlib.FwRegisters;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class CanalMaster
{

    public int controller;
    private FwMasterDevice workCanal = null;
    private FwRegisters tableDecode = null;
    public ArrayList<FwMasterDevice> mdarray = new ArrayList<>();
    private ArrayList<Socket> socketarray = new ArrayList<>();
    private long timeout=20000L;
    public CanalMaster(int controller, FwRegisters tableDecode, long timeout)
    {
        this.controller = controller;
        this.tableDecode = tableDecode;
        this.timeout=timeout;
    }

    public void addDevice(Socket socket)
    {
        try
        {
            socket.setSoTimeout((int) timeout);
            socketarray.add(socket);
            FwMasterDevice md = new FwMasterDevice(socket, controller, tableDecode);
            workCanal = md;
            mdarray.add(md);
        }
        catch (SocketException ex)
        {
            Log.CORE.info("Set timeOut "+ex);
        }
    }
    public void reconnectDevice(int idx)
    {
        try
        {
            //Log.CORE.info("Создаем заново ");
            Socket socket = new Socket(socketarray.get(idx).getInetAddress(), socketarray.get(idx).getPort());
            socket.setSoTimeout((int) timeout);
            FwMasterDevice md = new FwMasterDevice(socket, controller, tableDecode);
            if(md==null) return;
            workCanal = md;
            //Log.CORE.info("Создали заново "+md.myAddress());
            socketarray.set(idx, socket);
            mdarray.set(idx, md);
        }
        catch (IOException ex)
        {
            Log.CORE.info("Reconect "+ex);
        }
    }

    public void changeCanal()
    {
        workCanal = null;
        /*
        if (FwUtil.FP_DEBUG)
        {
            Log.CORE.info("=======================");
        }
        */
        for (FwMasterDevice master : mdarray)
        {
            /*
            if (FwUtil.FP_DEBUG)
            {
                Log.CORE.info(master.myAddress() + "/" + Integer.toString(controller) + " errors=" + Integer.toString(master.error) + " "
                        + Integer.toHexString(master.getErrFuncCode()) + (master.isConnected() ? " connected" : " disconnected"));
            }
            if (FwUtil.FP_DEBUG)
            {
                Log.CORE.info("Transport " + Integer.toString(master.mytransport.error) + " " + master.mytransport.textError);
            }
            if (FwUtil.FP_DEBUG)
            {
                Log.CORE.info(FwUtil.textError);
            }
            */
            if (master.isConnected())
            {
                workCanal = master;
            }
        }
        /*
        for (FwRegister reg : tableDecode.getCollection())
        {
            if (FwUtil.FP_DEBUG)
            {
                Log.CORE.info(reg.toString());
            }
        }
        */

    }

    public String nameWorkCanal()
    {
        if (workCanal == null)
        {
            return null;
        }
        return workCanal.myAddress();
    }

    public void clearDatas()
    {
        for (FwMasterDevice master : mdarray)
        {
            while (master.getHistory() != null);
        }
    }

    public FwOneReg getHistory()
    {
        if (workCanal == null)
        {
            return null;
        }
        return workCanal.getHistory();
    }
}
