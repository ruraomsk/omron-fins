/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

import java.io.IOException;
import java.util.Date;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Юрий
 */
public class FwTest
{

    public FwTest()
    {
    }

    @BeforeClass
    public static void setUpClass()
    {
    }

    @AfterClass
    public static void tearDownClass()
    {
    }

    @Before
    public void setUp()
    {
    }

    @After
    public void tearDown()
    {
    }

    /**
     * Test of toBuffer method, of class FwInfo.
     */
    @Test
    public void testFwDiag()
    {
        System.out.println("testFwDiag");
        byte[] buffer = new byte[1000];
        int controller=1;
        String UPCM="UPC is worked correctly!";
        FwRegisters tableDecode = new FwRegisters();
        tableDecode.add(new FwRegister(controller, false, 1, FwUtil.FP_TYPE_INTGER));
        tableDecode.add(new FwRegister(controller, false, 2, FwUtil.FP_TYPE_INTGER));
        tableDecode.add(new FwRegister(controller, false, 3, FwUtil.FP_TYPE_INTGER));
        tableDecode.add(new FwRegister(controller, false, 4, FwUtil.FP_TYPE_INTGER));
        tableDecode.add(new FwRegister(controller, false, 5, FwUtil.FP_TYPE_INTGER));
        tableDecode.add(new FwRegister(controller, false, 6, FwUtil.FP_TYPE_INTGER));
        FwDiag inst=new FwDiag(controller,tableDecode);
        for (int i = 0; i < 6; i++)
        {
            inst.setOneDiag(i,100+i);
        }
        inst.setUPCMessage(UPCM);
        int len = inst.toBuffer(buffer, 0);
        FwDiag result = null;
        result = new FwDiag(len, buffer, controller, tableDecode);
        for (int i = 0; i < inst.getSize(); i++)
        {
            assertEquals(inst.getDiagUId(i), result.getDiagUId(i));
            assertEquals(inst.getDiagCode(i), result.getDiagCode(i));
        }
        //assertEquals(UPCM,result.getUPCMessage());
        System.out.println("testFwDiag done");
        
    }
    @Test
    public void testFwInfo() throws InterruptedException
    {
        System.out.println("testFwInfo");
        byte[] buffer = new byte[1000];
        int tpos = 0;
        int controller = 0;
        int nomer = 1;
        FwRegisters tableDecode = new FwRegisters();
        tableDecode.add(new FwRegister(controller, 1, FwUtil.FP_TYPE_BOOL));
        tableDecode.add(new FwRegister(controller, 2, FwUtil.FP_TYPE_BOOL));
        tableDecode.add(new FwRegister(controller, 3, FwUtil.FP_TYPE_INTGER));
        tableDecode.add(new FwRegister(controller, 4, FwUtil.FP_TYPE_INTGER));
        tableDecode.add(new FwRegister(controller, 5, FwUtil.FP_TYPE_FLOAT));
        tableDecode.add(new FwRegister(controller, 6, FwUtil.FP_TYPE_FLOAT));
        Date now = new Date();
        FwInfo instance = new FwInfo(controller, nomer);
        for (FwRegister reg : tableDecode.getCollection())
        {
            instance.addOneReg(new FwOneReg(now, reg));
        }
        Thread.sleep(500L);
        now = new Date();
        for (FwRegister reg : tableDecode.getCollection())
        {
            instance.addOneReg(new FwOneReg(now, reg));
        }
        Thread.sleep(500L);
        now = new Date();
        for (FwRegister reg : tableDecode.getCollection())
        {
            instance.addOneReg(new FwOneReg(now, reg));
        }
        int len = instance.toBuffer(buffer, 0);
        FwInfo result = null;
        result = new FwInfo(len, buffer, controller, tableDecode);
        for (int i = 0; i < instance.getSize(); i++)
        {
            assertEquals(instance.getOneReg(i).getValue(), result.getOneReg(i).getValue());

        }
        System.out.println("testFwInfo done");
    }

    @Test
    public void TestFwKvit()
    {

        System.out.println("testFwKvit");
        FwKvit kvout = new FwKvit(0);
        kvout.setCmd(1);
        kvout.setNomFile(2);
        kvout.setNomer(3);
        kvout.setRezult(4);
        byte[] buffer = new byte[1000];
        kvout.toBuffer(buffer, 0);
        FwKvit kvin = new FwKvit(buffer,0);
        assertEquals(kvin.getCmd(), 1);
        assertEquals(kvin.getNomFile(), 2);
        assertEquals(kvin.getNomer(), 3);
        assertEquals(kvin.getRezult(), 4);
        assertEquals(kvout.getController(),kvin.getController());
        System.out.println("testFwKvit done");

    }
    @Test
    public void TestFwSyncTime() throws InterruptedException
    {

        System.out.println("testFwSyncTime");
        Date d1=new Date();
        Thread.sleep(500L);
        Date d2=new Date();
        
        FwSyncTime ss=new FwSyncTime (0,d1,d2);
        byte[] buffer = new byte[1000];
        ss.toBuffer(buffer, 0);
        FwSyncTime sin = new FwSyncTime (buffer,0);
        assertEquals(d1,sin.getDatelast());
        assertEquals(d2,sin.getDatenow());
        assertEquals(ss.getController(),sin.getController());
        System.out.println("testFwSyncTime done");

    }
    @Test
    public void TestFwMesCtrl() 
    {

        System.out.println("testFwMesCtrl");
        FwMesCtrl msout=new FwMesCtrl(1, 2);
        for (int i = 0; i < msout.getSize(); i++)
        {
            msout.setPar(i, i*10);
            msout.setGood(i, FwUtil.FP_DATA_NOGOOD);
        }
        
        byte[] buffer = new byte[1000];
        msout.toBuffer(buffer, 0);
        FwMesCtrl msin=new FwMesCtrl(buffer, 1);
        assertEquals(msout.getController(),msin.getController());
        assertEquals(2,msin.getCommandcode());
        for (int i = 0; i < msout.getSize(); i++)
        {
            assertEquals(msout.getPar(i),msin.getPar(i));
            assertEquals(msout.getGood(i),msin.getGood(i));
        }
       
        System.out.println("testFwMesCtrl done");

    }
    @Test
    public void TestFwMesLive() 
    {

        System.out.println("testFwMesLive");
        FwMesLive msout=new FwMesLive(1);
        msout.setCounter(2000);
        byte[] buffer = new byte[1000];
        msout.toBuffer(buffer, 0);
        FwMesLive msin=new FwMesLive(buffer, 1);
        assertEquals(msout.getController(),msin.getController());
        assertEquals(msout.getController(),msin.getController());
       
        System.out.println("testFwMesLive done");

    }
    @Test
    public void TestCRC(){
    byte[] buffer = new byte[1000];
    buffer[0]=1;
    buffer[1]=0;
    buffer[2]=2;    
            assertEquals(1,FwUtil.Crc(buffer, 0, 2));
            assertEquals(3,FwUtil.Crc(buffer, 0, 3));
    
    int len=buffer.length-10;
        for (int i = 0; i < buffer.length; i++)
        {
             buffer[i]=(byte) (i&0xff);
        }
    }
    @Test
    public void TestTransport() throws IOException 
    {

        System.out.println("testTransport");
/*
        FwMesLive msout=new FwMesLive(1);
        msout.setCounter(2000);
        
        FwMessage ms=new FwMessage( msout);
        FwRegisters tableDecode = new FwRegisters();
        FwResponse rs;
        synchronized (FwUtil.buffer)
        {
            System.arraycopy(FwUtil.outbuf, 5, FwUtil.buffer, 0, 1000);
            Integer len=FwUtil.ToShort(FwUtil.outbuf, 0);
            Integer controller=FwUtil.ToShort(FwUtil.outbuf, 2);
            byte functionCode=FwUtil.outbuf[4];
            System.out.println("buffer len " +len.toString()+" "+controller.toString()+" "+Integer.toHexString(functionCode));
            rs=new FwResponse(controller, functionCode, len, FwUtil.buffer, tableDecode);
        }
        FwMesLive msin=rs.getMesLive();
        assertEquals(msout.getCounter(),msin.getCounter());
        assertEquals(msout.getController(),msin.getController());
*/       
        System.out.println("testTransport done");

    }

}
