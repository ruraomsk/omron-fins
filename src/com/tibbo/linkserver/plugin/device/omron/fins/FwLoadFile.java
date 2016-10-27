/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tibbo.linkserver.plugin.device.omron.fins;

import com.tibbo.aggregate.common.Log;
import com.tibbo.aggregate.common.context.DefaultContextEventListener;
import com.tibbo.aggregate.common.data.Event;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.device.DeviceContext;
import com.tibbo.aggregate.common.event.EventHandlingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import ru.list.ruraomsk.fwlib.FwKvit;
import ru.list.ruraomsk.fwlib.FwMasterDevice;
import ru.list.ruraomsk.fwlib.FwSetup;
import ru.list.ruraomsk.fwlib.FwUtil;

/**
 * Загрузчик файлов в устройство
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class FwLoadFile extends DefaultContextEventListener
{

    ConcurrentHashMap<Integer, FwState> map;
    DeviceContext deviceContext;
    OmronFinsDeviceDriver fd;
    boolean first = true;

    FwLoadFile(DeviceContext deviceContext, OmronFinsDeviceDriver fd)
    {
        this.deviceContext = deviceContext;
        this.fd = fd;
        map = new ConcurrentHashMap<>();
        this.setCallerController(deviceContext.getCallerController());
    }

    public void regEventListner()
    {
        if (first) {
            deviceContext.addEventListener("Event_35H", this);
            deviceContext.addEventListener("Event_36H", this);
            
            first = false;
        }
    }

    public boolean addLoad(int controller, int nom_file, String nameFile)
    {
        try {
            FileInputStream inFile = new FileInputStream(new File(nameFile));
            byte[] buf = new byte[inFile.available()];
            inFile.read(buf);
            inFile.close();
            FwState st = new FwState(buf, nom_file);
            putmessage(controller, st);
            map.put(controller, st);
        }
        catch (IOException ex) {
            return false;
        }
        return true;
    }

    public boolean startLoad(int controller, int nom_file)
    {
        map.remove(controller);
        FwState st = new FwState(nom_file);
        for (CanalMaster cm : fd.canals) {
            if (cm.getController() != controller) {
                continue;
            }
            st.setNomer(1);
            st.cmd = FwUtil.FW_CMD_RECIVE;
            FwSetup outmess = new FwSetup(true, controller, st.nom_file, st.nomer, st.cmd);
            for (FwMasterDevice dev : cm.mdarray) {
                dev.putMessage(outmess);
            }
            map.put(controller, st);
            return true;
        }
        return false;

    }

    public boolean getFile(int controller, int nom_file, String nameFile)
    {
        try {
            FwState st = map.get(controller);
            if (st == null) {
                return false;
            }
            if (st.inout) {
                return false;
            }
            if (st.cmd != FwUtil.FW_CMD_LAST || st.rezult != 0) {
                return false;
            }
            FileOutputStream outFile = new FileOutputStream(nameFile);
            outFile.write(st.buffer, 0, st.length);
            outFile.close();
            map.remove(controller);
        }
        catch (IOException ex) {
            return false;
        }
        return true;
    }

    public String Status(int controller)
    {
        FwState st = map.get(controller);
        if (st == null) {
            return "Нет операций по контроллеру";
        }
        return (st.inout ? "Передаем файл " : "Принимаем файл ") + Integer.toString(st.nom_file) + " порция=" + Integer.toString(st.nomer)
                + " команда=" + Integer.toString(st.cmd) + " код завершения=" + Integer.toString(st.rezult);
    }

    @Override
    public void handle(Event event) throws EventHandlingException
    {
//        Log.CORE.info("Пришли в обработчик");
        DataTable ed = event.getData();
        if (event.getName().equals("Event_35H")) {
            for (DataRecord rd : ed) {
                int controller = rd.getInt("Controller");
                int nom_file = rd.getInt("file");
                int nomer = rd.getInt("nomer");
                int command = rd.getInt("command");
                FwState st = map.get(controller);
                if (st == null) {
                    Log.CORE.info("Нет такого контроллера " + Integer.toString(controller));
                    continue;
                }
                if (nom_file != st.nom_file || nomer != st.nomer) {
                    Log.CORE.info("Для контроллера " + Integer.toString(controller) + " неверная квитанция");
                    continue;
                }
                st.rezult = rd.getInt("result");
                if (st.rezult != 0) {
                    st.back();
                    if (++st.count > 3) {
                        Log.CORE.info("Для контроллера " + Integer.toString(controller) + " очень много ошибок");
                        continue;
                    }
                    putmessage(controller, st);
                    continue;
                }
                st.count = 0;
                if (st.cmd == FwUtil.FW_CMD_NEXT) {
                    putmessage(controller, st);
                    continue;
                }
            }
        }
        if (event.getName().equals("Event_36H")) {
            for (DataRecord rd : ed) {
                int controller = rd.getInt("Controller");
                FwState st = map.get(controller);
                if (st == null) {
                    Log.CORE.info("Нет такого контроллера " + Integer.toString(controller));
                    continue;
                }
                st.nom_file = rd.getInt("file");
                st.nomer = rd.getInt("nomer");
                st.cmd = rd.getInt("command");
                int lenght = rd.getInt("lenght");
                if (st.cmd == FwUtil.FW_CMD_NEXT || st.cmd == FwUtil.FW_CMD_LAST) {
                    byte[] buff = rd.getString("buffer").getBytes();
                    System.err.println(Arrays.toString(buff));
                    if (lenght > buff.length) {
                        lenght = buff.length;
                    }
                    System.arraycopy(buff, 0, st.buffer, st.pos, lenght);
                    st.setPos(lenght);
                    st.length += lenght;
                    FwKvit outmess = new FwKvit(controller);
                    outmess.setCmd(st.cmd);
                    outmess.setNomer(st.nomer);
                    outmess.setNomFile(st.nom_file);
                    outmess.setRezult(0);
                    for (CanalMaster cm : fd.canals) {
                        if (controller != cm.getController()) {
                            continue;
                        }
                        for (FwMasterDevice dev : cm.mdarray) {
                            dev.putMessage(outmess);
                        }
                    }

                }
            }
        }

    }

    private void putmessage(int controller, FwState st)
    {
        for (CanalMaster cm : fd.canals) {
            if (cm.getController() != controller) {
                continue;
            }
            byte[] buff;
            if ((st.pos + FwUtil.FW_CMD_SIZE) >= st.buffer.length) {
                st.cmd = FwUtil.FW_CMD_LAST;
                buff = new byte[st.buffer.length - st.pos];
            }
            else {
                st.cmd = FwUtil.FW_CMD_NEXT;
                buff = new byte[FwUtil.FW_CMD_SIZE];
            }
            System.arraycopy(st.buffer, st.pos, buff, 0, buff.length);
            st.setPos(buff.length);
            st.setNomer(1);
            FwSetup outmess = new FwSetup(true, controller, st.nom_file, st.nomer, st.cmd);
            outmess.setData(buff, buff.length);
            for (FwMasterDevice dev : cm.mdarray) {
                dev.putMessage(outmess);
            }
            return;
        }
    }

    class FwState
    {

        public byte[] buffer;
        public int nomer;
        public int nom_file;
        public int pos;
        public int cmd;
        private int prev_pos;
        private int prev_nomer;
        public int count = 0;
        public int rezult = 0;
        public boolean inout = true;
        public int length;

        FwState(byte[] buff, int nom_file)
        {
            buffer = new byte[buff.length];
            System.arraycopy(buff, 0, buffer, 0, buff.length);
            this.nom_file = nom_file;
            pos = 0;
            nomer = 0;
        }

        FwState(int nom_file)
        {
            buffer = new byte[FwUtil.MAX_LEN];
            this.nom_file = nom_file;
            length = 0;
            pos = 0;
            nomer = 0;
            inout = false;
        }

        public void setPos(int i)
        {
            prev_pos = pos;
            pos += i;
        }

        public void setNomer(int i)
        {
            prev_nomer = nomer;
            nomer += i;
        }

        public void back()
        {
            nomer = prev_nomer;
            pos = prev_pos;
        }

    }
}
