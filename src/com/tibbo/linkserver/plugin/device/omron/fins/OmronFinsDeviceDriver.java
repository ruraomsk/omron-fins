/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tibbo.linkserver.plugin.device.omron.fins;

import com.tibbo.aggregate.common.Log;
import com.tibbo.aggregate.common.context.CallerController;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextUtils;
import com.tibbo.aggregate.common.context.VariableDefinition;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.datatable.validator.LimitsValidator;
import com.tibbo.aggregate.common.datatable.validator.ValidatorHelper;
import com.tibbo.aggregate.common.device.*;
import com.tibbo.aggregate.common.security.ServerPermissionChecker;
import com.tibbo.aggregate.common.util.AggreGateThread;
import com.tibbo.aggregate.common.util.ThreadManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.list.ruraomsk.fwlib.*;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class OmronFinsDeviceDriver extends AbstractDeviceDriver
{

    public OmronFinsDeviceDriver()
    {
        super("fwdevice", VFT_CONNECTION_PROPERTIES);
    }

    @Override
    public void setupDeviceContext(DeviceContext deviceContext)
            throws ContextException
    {
        super.setupDeviceContext(deviceContext);
        deviceContext.setDefaultSynchronizationPeriod(10000L);
        VariableDefinition vd = new VariableDefinition("connectionProperties", VFT_CONNECTION_PROPERTIES, true, true, "connectionProperties", ContextUtils.GROUP_ACCESS);
        vd.setIconId("var_connection");
        vd.setHelpId("ls_drivers_fwdevice");
        vd.setWritePermissions(ServerPermissionChecker.getManagerPermissions());
        deviceContext.addVariableDefinition(vd);

        vd = new VariableDefinition("registers", VFT_REGISTERS, true, true, "Регистры", ContextUtils.GROUP_ACCESS);
        vd.setWritePermissions(ServerPermissionChecker.getAdminPermissions());
        deviceContext.addVariableDefinition(vd);

        vd = new VariableDefinition("devices", VFT_DEVICES, true, true, "Устройства", ContextUtils.GROUP_ACCESS);
        vd.setWritePermissions(ServerPermissionChecker.getAdminPermissions());
        deviceContext.addVariableDefinition(vd);

        vd = new VariableDefinition("SQLProperties", VFT_SQL, true, true, "Сохранение дампа", ContextUtils.GROUP_ACCESS);
        vd.setWritePermissions(ServerPermissionChecker.getManagerPermissions());
        deviceContext.addVariableDefinition(vd);

        deviceContext.setDeviceType("fwdevice");
        // После отладки удалить
        //FwUtil.FP_DEBUG = true;
    }

    @Override
    public void connect() throws DeviceException
    {
        try
        {
            makeAllTables();
            canals = new ArrayList<>();
            DataRecord cp = getDeviceContext().getVariable("connectionProperties", getDeviceContext().getCallerController()).rec();
            DataTable devs = getDeviceContext().getVariable("devices", getDeviceContext().getCallerController());
            devs.sort("controller", true);
            int controller = -1;
            for (DataRecord dev : devs)
            {
                try
                {

                    if (controller != dev.getInt("controller"))
                    {
                        controller = dev.getInt("controller");
                        canals.add(new CanalMaster(controller, tableDecode, cp.getLong("timeOut")));
                    }
                    CanalMaster cm = canals.get(canals.size() - 1);
                    Socket socket = new Socket(InetAddress.getByName(dev.getString("IPaddr")), dev.getInt("port"));
                    cm.addDevice(socket);
                    canals.set(canals.size() - 1, cm);
                }
                catch (IOException ex)
                {
                    Log.CORE.info("Devices error " + ex.getMessage());
                }
            }
            for (CanalMaster cm : canals)
            {
                for (FwMasterDevice dev : cm.mdarray)
                {
                    try
                    {
                        dev.setStepTime(cp.getLong("stepTime"));
                        dev.setStepGiveMe(cp.getLong("giveTime"));
                        dev.setStepLive(cp.getLong("liveTime"));
                        dev.gogo();
                        if (dev.isConnected())
                        {
                            if (FwUtil.FP_DEBUG)
                            {
                                Log.CORE.info("Device is connected " + dev.myAddress());
                            }
                            if (FwUtil.FP_DEBUG)
                            {
                                Log.CORE.info(Long.toString(dev.getStepTime()) + " " + Long.toString(dev.getStepGiveMe()) + " " + Long.toString(dev.getStepLive()));
                            }
                        } else
                        {
                            Log.CORE.info("Device is NOT connected " + dev.myAddress());
                        }
                    }
                    catch (Exception ex)
                    {
                        Log.CORE.info("Devices error " + ex.getMessage());
                    }
                }
            }
        }
        catch (ContextException ex)
        {
            Log.CORE.info("Devices error " + ex.getMessage());
        }

        if (thReadRec == null)
        {
            thReadRec = new MultiReconect(this, thrManRec);
        }

        try
        {
            DataRecord sqlrec = getDeviceContext().getVariable("SQLProperties", getDeviceContext().getCallerController()).rec();
            yesSQL = sqlrec.getBoolean("yesSQL");
            if (!yesSQL)
            {
                super.connect();
                return;
            }
            myDB = sqlrec.getString("table");
            myDBH = myDB + "_head";
            Class.forName(sqlrec.getString("JDBC"));
            con = DriverManager.getConnection(sqlrec.getString("url"), sqlrec.getString("user"), sqlrec.getString("password"));
            stmt = con.createStatement();
            String rez = "SELECT * FROM " + myDBH + " WHERE id=1";
            ResultSet rr = stmt.executeQuery(rez);
            rr.next();
            MaxLenght = rr.getLong("max");
            TekPos = rr.getLong("pos");
            LastPos = rr.getLong("last");
        }
        catch (ContextException | ClassNotFoundException | SQLException ex)
        {
            yesSQL = false;
            Log.CORE.info(ex.getMessage());
            throw new DeviceException(ex.getMessage());
        }
        if (thReadSQL == null)
        {
            thReadSQL = new MultiSQL(this, thrManSQL);
        }
        super.connect();
    }
    private Connection con = null;
    private Statement stmt = null;
    private Long MaxLenght;
    private Long TekPos;
    private Long LastPos;
    private String myDB;
    private String myDBH;
    private boolean yesSQL = false;

    private ThreadManager thrManSQL = new ThreadManager();
    private MultiSQL thReadSQL = null;

    private ThreadManager thrManRec = new ThreadManager();
    private MultiReconect thReadRec = null;

    ;

    @Override
    public void disconnect() throws DeviceException
    {
        super.disconnect(); //To change body of generated methods, choose Tools | Templates.
        if (canals == null)
        {
            return;
        }
        for (CanalMaster cm : canals)
        {
            for (FwMasterDevice dev : cm.mdarray)
            {
                dev.disconect();
            }
        }
        tableDecode = null;
        canals = null;

        if (yesSQL)
        {
            yesSQL = false;
            try
            {
                con.close();
                stmt.close();
            }
            catch (SQLException ex)
            {
                throw new DeviceException(ex);
            }
        }
    }

    @Override
    public void startSynchronization() throws DeviceException
    {
        super.startSynchronization(); //To change body of generated methods, choose Tools | Templates.
        if (canals == null)
        {
            return;
        }
        for (CanalMaster cm : canals)
        {
            cm.changeCanal();
            FwOneReg oreg;
            while ((oreg = cm.getHistory()) != null)
            {
                String name = reversdata.get(oreg.getReg().getKey());
                data.put(name, oreg);
                history.add(oreg);
            }
            cm.clearDatas();
        }
    }

    @Override
    public void finishSynchronization() throws DeviceException, DisconnectionException
    {
        FwOneReg oreg;
        if (!yesSQL)
        {
            while ((oreg = history.poll()) != null);
            return;
        }
        HashMap<String, FwOneReg> tHm = new HashMap<>(FwUtil.VALUE_UIDS);
        super.finishSynchronization(); //To change body of generated methods, choose Tools | Templates.
        for (FwOneReg onereg : data.values())
        {
            String key = reversdata.get(onereg.getReg().getKey()) + Long.toString(onereg.getDate().getTime());
            if (!tHm.containsKey(key))
            {
                tHm.put(key, onereg);
            }
        }
        while ((oreg = history.poll()) != null)
        {
            String key = reversdata.get(oreg.getReg().getKey()) + Long.toString(oreg.getDate().getTime());
            if (!tHm.containsKey(key))
            {
                tHm.put(key, oreg);
            }
        }
        String rezult = "";
        for (FwOneReg onereg : tHm.values())
        {
            String vname = reversdata.get(onereg.getReg().getKey());
            rezult += "<" + vname + "=";

            Object obj = onereg.getValue();
            switch (obj.getClass().getName())
            {
                case "java.lang.Boolean":
                    rezult += ((Boolean) obj ? "1" : "0");
                    break;
                case "java.lang.Long":
                    rezult += Long.toString((long) obj);
                    break;
                case "java.lang.Integer":
                    rezult += Integer.toString((int) obj);
                    break;
                default:
                    rezult += Float.toString((Float) obj);
                    break;
            }
            rezult += "_" + Long.toString(onereg.getDate().getTime()) + ">";
        }
        Timestamp timestamp = new Timestamp(new Date().getTime());
        StSQL mySQL = new StSQL(timestamp, rezult);
        hSQL.add(mySQL);
    }

    @Override
    public DataTable readVariableValue(VariableDefinition vd, CallerController caller) throws ContextException, DeviceException, DisconnectionException
    {
        FwOneReg oreg = data.get(vd.getName());
        DataTable res = new DataTable(vd.getFormat());
        if (oreg == null)
        {
            res.addRecord(0);
            return res;
        }
        res.addRecord(oreg.getValue());
        return res;
    }

    @Override
    public List<VariableDefinition> readVariableDefinitions(DeviceEntities entities) throws ContextException, DeviceException, DisconnectionException
    {
        List res = new LinkedList();
        makeAllTables();
        DataTable regs = getDeviceContext().getVariable("registers", getDeviceContext().getCallerController());
        for (DataRecord reg : regs)
        {
            char type = ' ';
            switch (reg.getInt("format"))
            {
                case 0:
                    type = 'B';
                    break;
                case 1:
                    type = 'I';
                    break;
                case 2:
                    type = 'F';
                    break;
            }

            FieldFormat ff = FieldFormat.create(reg.getString("name"), type, reg.getString("description"));
            TableFormat tf = new TableFormat(1, 1, ff);
            tf.setUnresizable(true);
            VariableDefinition vd = new VariableDefinition(reg.getString("name"), tf, true, false, reg.getString("description"), "remote");
            res.add(vd);
        }
        return res;
    }

    private void makeAllTables()
    {
        if (tableDecode != null)
        {
            return;
        }
        try
        {
            Date now = new Date();
            history = new ConcurrentLinkedQueue<>();
            DataTable regs = getDeviceContext().getVariable("registers", getDeviceContext().getCallerController());
            tableDecode = new FwRegisters(FwUtil.VALUE_UIDS);
            data = new ConcurrentHashMap<>(FwUtil.VALUE_UIDS);
            reversdata = new ConcurrentHashMap<>(FwUtil.VALUE_UIDS);
            for (DataRecord reg : regs)
            {
                FwRegister fwreg = new FwRegister(reg.getInt("canal"), reg.getInt("type") == 0, reg.getInt("uId"), reg.getInt("format"));
                tableDecode.add(fwreg);
                FwOneReg oreg = new FwOneReg(now, fwreg);
                data.put(reg.getString("name"), oreg);
                reversdata.put(oreg.getReg().getKey(), reg.getString("name"));
            }
        }
        catch (ContextException ex)
        {
            Log.CORE.info("Registers not found" + ex.getMessage());
        }

    }
    private ConcurrentHashMap<String, FwOneReg> data = null;
    private ConcurrentHashMap<Long, String> reversdata = null;

    private ConcurrentLinkedQueue<FwOneReg> history = null;
    private FwRegisters tableDecode = null;
    private ArrayList<CanalMaster> canals = null;
    private static final TableFormat VFT_CONNECTION_PROPERTIES;
    private static final TableFormat VFT_REGISTERS;
    private static final TableFormat VFT_DEVICES;
    private static final TableFormat VFT_SQL;

    private static Map TypeSelectionValues()
    {
        Map types = new LinkedHashMap();
        types.put(0, "Информация");
        types.put(1, "Диагностика");
        return types;
    }

    private static Map FormatSelectionValues()
    {
        Map reg = new LinkedHashMap();
        reg.put(0, "Логический");
        reg.put(1, "2-байтный Int Signed");
        reg.put(2, "4-байтный Float");
        return reg;
    }

    static
    {
        VFT_CONNECTION_PROPERTIES = new TableFormat(1, 1);

        VFT_CONNECTION_PROPERTIES.addField(FieldFormat.create("<stepTime><L><A=500><D=Период опроса устройств>"));
        VFT_CONNECTION_PROPERTIES.addField(FieldFormat.create("<giveTime><L><A=10000><D=Интервал обновления информации о переменных>"));
        VFT_CONNECTION_PROPERTIES.addField(FieldFormat.create("<liveTime><L><A=10000><D=Интервал обновления отметки жизни>"));
        VFT_CONNECTION_PROPERTIES.addField(FieldFormat.create("<timeOut><L><A=20000><D=Стандартный тайм-аут ввода/вывода>"));

        VFT_REGISTERS = new TableFormat(true);
        FieldFormat ff = FieldFormat.create("<name><S><D=Имя>");
        ff.getValidators().add(ValidatorHelper.NAME_LENGTH_VALIDATOR);
        ff.getValidators().add(ValidatorHelper.NAME_SYNTAX_VALIDATOR);
        VFT_REGISTERS.addField(ff);
        ff = FieldFormat.create("<description><S><D=Описание>");
        ff.getValidators().add(new LimitsValidator(1, 200));
        ff.getValidators().add(ValidatorHelper.DESCRIPTION_SYNTAX_VALIDATOR);
        VFT_REGISTERS.addField(ff);
        VFT_REGISTERS.addField(FieldFormat.create("<canal><I><A=0><D=Номер канала>"));
        ff = FieldFormat.create("<type><I><D=Тип>");
        ff.setSelectionValues(TypeSelectionValues());
        VFT_REGISTERS.addField(ff);
        VFT_REGISTERS.addField(FieldFormat.create("<uId><I><A=0><D=Идентификатор>"));
        ff = FieldFormat.create("<format><I><D=Формат>");
        ff.setSelectionValues(FormatSelectionValues());
        VFT_REGISTERS.addField(ff);

        VFT_DEVICES = new TableFormat(true);
        VFT_DEVICES.addField(FieldFormat.create("<controller><I><D=Номер контроллера>"));
        VFT_DEVICES.addField(FieldFormat.create("<IPaddr><S><D=IP address контроллера>"));
        VFT_DEVICES.addField(FieldFormat.create("<port><I><A=502><D=Номер порта>"));

        VFT_SQL = new TableFormat(1, 1);
        VFT_SQL.addField(FieldFormat.create("<yesSQL><B><A=false><D=Включить сохранение дампа>"));
        VFT_SQL.addField(FieldFormat.create("<url><S><A=jdbc:mysql://localhost:3306/cyclebuff><D=Url базы данных дампов>"));
        VFT_SQL.addField(FieldFormat.create("<JDBC><S><A=com.mysql.jdbc.Driver><D=Драйвер базы данных>"));
        VFT_SQL.addField(FieldFormat.create("<table><S><A=buffer><D=Таблица дампа>"));
        VFT_SQL.addField(FieldFormat.create("<user><S><D=Пользователь>"));
        VFT_SQL.addField(FieldFormat.create("<password><S><D=Пароль>"));
    }
    private ConcurrentLinkedQueue<StSQL> hSQL = new ConcurrentLinkedQueue<StSQL>();

    public class StSQL
    {

        Timestamp timestamp;
        String var;

        StSQL(Timestamp timestamp, String var)
        {
            this.timestamp = timestamp;
            this.var = var;
        }

        public Timestamp getTimestamp()
        {
            return timestamp;
        }

        public String getVar()
        {
            return var;
        }

    }

    class MultiReconect extends AggreGateThread
    {

        private OmronFinsDeviceDriver fd;
        ThreadManager threadManager = null;

        public MultiReconect(OmronFinsDeviceDriver fd, ThreadManager threadManager)
        {
            super(threadManager);
            this.threadManager = threadManager;
            threadManager.addThread(this);
            this.fd = fd;
            start();
        }

        @Override
        public void run()
        {
            do
            {
                try
                {
                    DataRecord cp = getDeviceContext().getVariable("connectionProperties", getDeviceContext().getCallerController()).rec();

                    for (CanalMaster cm : canals)
                    {
                        for (int idx = 0; idx < cm.mdarray.size(); idx++)
                        {
                            FwMasterDevice dev = cm.mdarray.get(idx);
                            if (dev == null)
                            {
                                continue;
                            }
                            //Log.CORE.info(" Проверяем " + dev.myAddress());
                            if (dev.isConnected())
                            {
                                continue;
                            }
                            cm.reconnectDevice(idx);
                            dev = cm.mdarray.get(idx);
                            if (dev == null)
                            {
                                continue;
                            }
                            dev.setStepTime(cp.getLong("stepTime"));
                            dev.setStepGiveMe(cp.getLong("giveTime"));
                            dev.setStepLive(cp.getLong("liveTime"));
                            dev.gogo();
                            if (dev.isConnected())
                            {
                                if (FwUtil.FP_DEBUG)
                                {
                                    Log.CORE.info("Device is connected " + dev.myAddress());
                                }
                                if (FwUtil.FP_DEBUG)
                                {
                                    Log.CORE.info(Long.toString(dev.getStepTime()) + " " + Long.toString(dev.getStepGiveMe()) + " " + Long.toString(dev.getStepLive()));
                                }
                            }
                        }
                    }
                }
                catch (Exception ex)
                {
                    Log.CORE.info("Reconnect Exception " + ex.getMessage());
                }
                try
                {
                    AggreGateThread.sleep(10000L);
                }
                catch (InterruptedException ex)
                {
                    //Log.CORE.info("stop driver ");
                    return;
                }

            }
            while (!isInterrupted());
        }
    }

    class MultiSQL extends AggreGateThread
    {

        private OmronFinsDeviceDriver fd;
        ThreadManager threadManager = null;

        public MultiSQL(OmronFinsDeviceDriver fd, ThreadManager threadManager)
        {
            super(threadManager);
            this.threadManager = threadManager;
            threadManager.addThread(this);
            this.fd = fd;
            start();
        }

        @Override
        public void run()
        {
            do
            {
                if (!yesSQL)
                {
                    return;
                }
                try
                {
                    if (hSQL.size() > 3)
                    {
                        Log.CORE.info("в очереди на запись запросов " + hSQL.size());
                    }
                    StSQL mySt;
                    mySt = hSQL.poll();
                    while (mySt != null)
                    {
                        String rez;
                        if (LastPos > MaxLenght)
                        {
                            rez = "UPDATE " + myDB + " SET tm='" + mySt.getTimestamp().toString() + "',var='" + mySt.var + "' WHERE id=" + TekPos.toString() + ";";
                            TekPos++;
                        } else
                        {
                            rez = "INSERT INTO " + myDB + "(id,tm,var) VALUES( " + TekPos.toString() + ",'" + mySt.getTimestamp().toString() + "','" + mySt.var + "')";
                            LastPos++;
                            TekPos++;
                        }
                        stmt.executeUpdate(rez);
                        if (TekPos > MaxLenght)
                        {
                            TekPos = 0L;
                        }
                        rez = "UPDATE " + myDBH + " SET pos=" + TekPos.toString() + ", last=" + LastPos.toString() + " WHERE id=1";
                        stmt.executeUpdate(rez);
                        mySt = hSQL.poll();
                    }

                    AggreGateThread.sleep(500L);
                }
                catch (InterruptedException ex)
                {
                    //Log.CORE.info("stop driver ");
                    return;
                }
                catch (SQLException ex)
                {
                    Log.CORE.info("SqlException " + ex.getMessage());
                }
            }
            while (!isInterrupted());
        }
    }

}
