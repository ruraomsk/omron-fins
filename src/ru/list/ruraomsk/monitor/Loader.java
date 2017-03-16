/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.monitor;

import com.tibbo.aggregate.common.datatable.DataRecord;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import ru.list.ruraomsk.fwlib.FwMasterDevice;
import ru.list.ruraomsk.fwlib.FwRegister;
import ru.list.ruraomsk.fwlib.FwRegisters;
import ru.list.ruraomsk.fwlib.FwSlaveDevice;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class Loader
{

    private HashMap<Integer, FwSlaveDevice> sl = new HashMap<>();
    private HashMap<Integer, String> slname = new HashMap<>();
    private HashMap<Integer, SlaveDisplay> sld = new HashMap<>();
    private HashMap<Integer, FwMasterDevice> ms = new HashMap<Integer, FwMasterDevice>();
    private HashMap<Integer, String> msname = new HashMap<Integer, String>();
    private HashMap<Integer, MasterDisplay> mld = new HashMap<>();

    Loader() throws IOException, InterruptedException
    {
        Monitor.appendMessage("Начало работы");

        FwRegisters tableDecode = new FwRegisters(Monitor.registers.getRecordCount());
        Monitor.globalName = new HashMap<>(Monitor.registers.getRecordCount());
        for(DataRecord rec:Monitor.registers){
            FwRegister reg = new FwRegister(rec.getInt("canal"), rec.getInt("type")==0, rec.getInt("uId"), rec.getInt("format"),rec.getInt("lenght"));
            Monitor.globalName.put(reg.getKey(), rec.getString("name"));
            tableDecode.add(reg);
        }
        Monitor.appendMessage("Загрузили таблицу имен");
        for(DataRecord dev:Monitor.devices){
            String name=dev.getString("idvlr");
            Integer controller=dev.getInt("controller");
            slname.put(controller,name);
            Socket socket=new Socket(Monitor.IPhost, dev.getInt("port"));
                FwSlaveDevice sd = new FwSlaveDevice(socket,controller, tableDecode);
                sl.put(controller,sd);
                SlaveDisplay dd = new SlaveDisplay(name, sd);
                sld.put(controller, dd);
                Monitor.appendMessage("Создано устройство " + name
                        + " контроллер=" + controller.toString()
                        + " "+Monitor.IPhost+":" + dev.getInt("port").toString());
            
        }
    }

}
