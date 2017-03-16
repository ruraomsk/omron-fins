/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tibbo.linkserver.plugin.device.omron.fins;

import com.tibbo.aggregate.common.Log;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.list.ruraomsk.fwlib.FwBaseMess;
import ru.list.ruraomsk.fwlib.FwMasterDevice;
import ru.list.ruraomsk.fwlib.FwOneReg;
import ru.list.ruraomsk.fwlib.FwRegisters;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class CanalMaster extends Thread {

    public int controller;
    private FwMasterDevice workCanal = null;
    private FwRegisters tableDecode = null;
    public ArrayList<FwMasterDevice> mdarray = new ArrayList<>();
    public ArrayList<Socket> socketarray = new ArrayList<>();
    private long timeout = 20000L;
    private int port;
    private boolean master = true;

    public CanalMaster(int controller, FwRegisters tableDecode, long timeout) {
        this.controller = controller;
        this.tableDecode = tableDecode;
        this.timeout = timeout;
    }

    /**
     * Устанавливает данный канал пассивным
     *
     * @param port - номер порта
     */
    public void setSlave(int port) {
        this.port = port;
        master = false;
        start();
    }

    public boolean isMaster() {
        return master;
    }

    /**
     * Добавляет устройство в канал
     *
     * @param socket
     * @return
     */
    public FwMasterDevice addDevice(Socket socket) {
        try {
            socket.setSoTimeout((int) timeout);
            socketarray.add(socket);
            FwMasterDevice md = new FwMasterDevice(socket, controller, tableDecode);
            workCanal = md;
            synchronized (mdarray) {
                mdarray.add(md);
            }
            return md;
        } catch (SocketException ex) {
            Log.CORE.error("Set timeOut " + ex);
            return null;
        }
    }

    /**
     * переподключает устройство если канал активный
     *
     * @param idx
     */
    public void reconnectDevice(int idx) {
        if (!master) {
            return;
        }
        try {
            Socket socket = new Socket(socketarray.get(idx).getInetAddress(), socketarray.get(idx).getPort());
            socket.setSoTimeout((int) timeout);
            FwMasterDevice md = new FwMasterDevice(socket, controller, tableDecode);
            workCanal = md;
            Log.CORE.info("Создали заново " + md.myAddress());
            synchronized (mdarray) {
                socketarray.set(idx, socket);
                mdarray.set(idx, md);
            }
        } catch (IOException ex) {
            Log.CORE.error("Reconect " + ex);
        }
    }

    /**
     * назначает основное устройство в канале
     */
    public void changeCanal() {
        workCanal = null;
        for (FwMasterDevice master : mdarray) {
            if (master.isConnected()) {
                workCanal = master;
            }
        }
    }

    public String nameWorkCanal() {
        if (workCanal == null) {
            return null;
        }
        return workCanal.myAddress();
    }

    public void clearDatas() {
        for (FwMasterDevice master : mdarray) {
            while (master.getHistory() != null);
        }
    }

    public FwOneReg getHistory() {
        if (workCanal == null) {
            return null;
        }
        return workCanal.getHistory();
    }

    public String getUPCMessage() {
        if (workCanal == null) {
            return null;
        }
        return workCanal.getUPCMessage();
    }

    public FwBaseMess readMessage() {
        if (workCanal == null) {
            return null;
        }
        return workCanal.readMessage();
    }

    public void putMessage(FwBaseMess message) {
        for (FwMasterDevice master : mdarray) {
            if (master.isConnected()) {
                master.putMessage(message);
            }
        }
    }

    public int getController() {
        return controller;
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (!isInterrupted()) {
                Socket socket = serverSocket.accept();
                FwMasterDevice md = addDevice(socket);
                md.gogo();
            }
        } catch (IOException ex) {
            Log.CORE.error("Ошибка в пассивном сокете " + ex.getMessage());
        }
    }

}
