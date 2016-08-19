/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;

/**
 * 
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class TestXML
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, InterruptedException
    {
        // TODO code application logic here
        HashMap<Integer, FwSlaveDevice> sl = new HashMap<Integer, FwSlaveDevice>();
        HashMap<Integer, String> slname = new HashMap<Integer, String>();
        HashMap<Integer, FwSlave> slm = new HashMap<Integer, FwSlave>();
        HashMap<Integer, FwMasterDevice> ms = new HashMap<Integer, FwMasterDevice>();
        HashMap<Integer, String> msname = new HashMap<Integer, String>();
        FwSpace fst = new FwSpace();
        try
        {
            javax.xml.bind.JAXBContext jaxbCtx = javax.xml.bind.JAXBContext.newInstance(fst.getClass().getPackage().getName());
            javax.xml.bind.Unmarshaller unmarshaller = jaxbCtx.createUnmarshaller();
            fst = (FwSpace) unmarshaller.unmarshal(new java.io.File("Spaces.xml")); //NOI18N
        }
        catch (javax.xml.bind.JAXBException ex)
        {
            // XXXTODO Handle exception
            java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.SEVERE, null, ex); //NOI18N
        }
        FwRegisters tableDecode = new FwRegisters(fst.getNames().getName().size());
        for (int i = 0; i < fst.getNames().getName().size(); i++)
        {
            NameNode nn = fst.getNames().getName().get(i);
            FwRegister reg = new FwRegister(nn.getController(), nn.isType(),nn.getUId(), nn.getFormat());
            tableDecode.add(reg);
        }
        for (int i = 0; i < fst.getDevices().getDevice().size(); i++)
        {
            DeviceNode dev = fst.getDevices().getDevice().get(i);
            if (dev.getIPadr().equals("none"))
            {
                //slname.put(dev.getController(), dev.getName());
                //sl.put(dev.getController(), new FwSlaveDevice(dev.getPort(), dev.getController(), tableDecode));
                //slm.put(dev.getController(), new FwSlave(dev.getPort()));
            } else
            {
                msname.put(dev.getController(), dev.getName());
                Socket socket = new Socket(InetAddress.getByName(dev.getIPadr()), dev.getPort());
                ms.put(dev.getController(), new FwMasterDevice(socket, dev.getController(), tableDecode));
            }
        }
        
        while (!Thread.interrupted())
        {
            /*
            for (FwSlaveDevice dev : sl.values())
            {
                for (FwRegister reg : tableDecode.getCollection())
                {
                    if ((reg.getController() == dev.getController()) && reg.isInfo())
                    {
                        if (dev.getValue(reg.getuId()) == null)
                        {
                            continue;
                        }
                        if(reg.getType()==FwUtil.FP_TYPE_BOOL){
                            boolean t=(boolean)dev.getValue(reg.getuId());
                            dev.setValue(reg.getuId(), !t);
                        }
                        if(reg.getType()==FwUtil.FP_TYPE_FLOAT){
                            float t=(float)dev.getValue(reg.getuId());
                            dev.setValue(reg.getuId(), t+1.2f);
                        }
                        if(reg.getType()==FwUtil.FP_TYPE_INTGER){
                            int t=(int)dev.getValue(reg.getuId());
                            dev.setValue(reg.getuId(), t+1);
                        }
                        
                        //System.out.println("Slave=" + slname.get(reg.getController())
                        //        + " uId=" + Integer.toString(reg.getuId()) + " value=" + dev.getValue(reg.getuId()).toString());
                    }
                    if ((reg.getController() == dev.getController()) && reg.isDiag())
                    {
                        if(reg.getType()==FwUtil.FP_TYPE_INTGER){
                            int t=(int)dev.getCode(reg.getuId());
                            dev.setCode(reg.getuId(), t+1);
                        }
                        
                        //System.out.println("Slave=" + slname.get(reg.getController())
                        //        + " uId=" + Integer.toString(reg.getuId()) + " value=" + dev.getValue(reg.getuId()).toString());
                    }
                }
                dev.sendDiag();
            }
            
            System.out.println("Обновили данные в устройствах");
            */
            for (FwMasterDevice dev : ms.values())
            {
                System.out.println("Master=" + msname.get(dev.getController()));
                FwOneReg temp= dev.getHistory();
                while (temp != null)
                {
                    System.out.println("info uId=" + Integer.toString(temp.getuId()) + " value=" + temp.getValue().toString());
                    temp = dev.getHistory();
                }
/*                FwDiagReg diag= dev.getDiagHistory();
                while (diag != null)
                {
                    System.out.println("Diag uId=" + Integer.toString(diag.getuId()) + " value=" + diag.getValue().toString());
                    diag = dev.getDiagHistory();
                }
*/
            }
            
            Thread.sleep(FwUtil.FP_STEP_TIME);
        }

    }

}
