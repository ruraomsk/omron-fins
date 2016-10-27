/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.monitor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import ru.list.ruraomsk.fwlib.DeviceNode;
import ru.list.ruraomsk.fwlib.FwMasterDevice;
import ru.list.ruraomsk.fwlib.FwRegister;
import ru.list.ruraomsk.fwlib.FwRegisters;
import ru.list.ruraomsk.fwlib.FwSlave;
import ru.list.ruraomsk.fwlib.FwSlaveDevice;
import ru.list.ruraomsk.fwlib.FwSpace;
import ru.list.ruraomsk.fwlib.NameNode;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class Loader
{

    private HashMap<Integer, FwSlaveDevice> sl = new HashMap<>();
    private HashMap<Integer, String> slname = new HashMap<>();
    private HashMap<Integer, FwSlave> slm = new HashMap<>();
    private HashMap<Integer, SlaveDisplay> sld = new HashMap<>();
    private HashMap<Integer, FwMasterDevice> ms = new HashMap<Integer, FwMasterDevice>();
    private HashMap<Integer, String> msname = new HashMap<Integer, String>();
    private HashMap<Integer, MasterDisplay> mld = new HashMap<>();

    Loader() throws IOException, InterruptedException
    {
        Monitor.appendMessage("Начало работы");
        FwSpace fst = new FwSpace();
        try
        {
            javax.xml.bind.JAXBContext jaxbCtx = javax.xml.bind.JAXBContext.newInstance(fst.getClass().getPackage().getName());
            javax.xml.bind.Unmarshaller unmarshaller = jaxbCtx.createUnmarshaller();
            fst = (FwSpace) unmarshaller.unmarshal(new java.io.File("Monitor.xml")); //NOI18N
        }
        catch (javax.xml.bind.JAXBException ex)
        {
            // XXXTODO Handle exception
            java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.SEVERE, null, ex); //NOI18N
        }
        Monitor.appendMessage("Создали классы из XML");

        FwRegisters tableDecode = new FwRegisters(fst.getNames().getName().size());
        Monitor.globalName = new HashMap<>(fst.getNames().getName().size());
        for (int i = 0; i < fst.getNames().getName().size(); i++)
        {
            NameNode nn = fst.getNames().getName().get(i);
            FwRegister reg = new FwRegister(nn.getController(), nn.isType(), nn.getUId(), nn.getFormat(),nn.getLenght());
            Monitor.globalName.put(reg.getKey(), nn.getName());
            tableDecode.add(reg);
        }
        Monitor.appendMessage("Загрузили таблицу имен");

        for (int i = 0; i < fst.getDevices().getDevice().size(); i++)
        {
            DeviceNode dev = fst.getDevices().getDevice().get(i);
            if (dev.getIPadr().equals("none"))
            {
                slname.put(dev.getController(), dev.getName());
                FwSlaveDevice sd = new FwSlaveDevice(dev.getPort(), dev.getController(), tableDecode);
                sl.put(dev.getController(), sd);
                slm.put(dev.getController(), new FwSlave(dev.getPort()));
                SlaveDisplay dd = new SlaveDisplay(dev.getName(), sd);
                sld.put(dev.getController(), dd);
                Monitor.appendMessage("Создано устройство " + dev.getName()
                        + " контроллер=" + dev.getController().toString()
                        + " порт=" + dev.getPort().toString());
            } else
            {
                msname.put(dev.getController(), dev.getName());
                Socket socket = new Socket(InetAddress.getByName(dev.getIPadr()), dev.getPort());
                FwMasterDevice md = new FwMasterDevice(socket, dev.getController(), tableDecode);
                md.setStepTime(500L);
                md.setStepGiveMe(10000L);
                md.setStepLive(10000L);
                md.gogo();
                ms.put(dev.getController(), md);
                MasterDisplay dd = new MasterDisplay(dev.getName(), md);
                mld.put(dev.getController(), dd);
                Monitor.appendMessage("Создано устройство " + dev.getName()
                        + " контроллер=" + dev.getController().toString()
                        + " IPAddrwss=" + dev.getIPadr()
                        + " порт=" + dev.getPort().toString());
            }
        }
    }

}
