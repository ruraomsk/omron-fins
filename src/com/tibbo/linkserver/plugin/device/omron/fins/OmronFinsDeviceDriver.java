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
import com.tibbo.aggregate.common.context.EventDefinition;
import com.tibbo.aggregate.common.context.FunctionDefinition;
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
//        deviceContext.setDeviceType("com.tibbo.linkserver.plugin.device.omron-fins");
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
        ldF = new FwLoadFile(deviceContext, this);
        // После отладки удалить
        //FwUtil.FP_DEBUG = true;
    }
    FwLoadFile ldF;

    @Override
    public void connect() throws DeviceException
    {
        try {
            errorSQL = true;
            makeAllTables();
            canals = new ArrayList<>();
            DataRecord cp = getDeviceContext().getVariable("connectionProperties", getDeviceContext().getCallerController()).rec();
            DataTable devs = getDeviceContext().getVariable("devices", getDeviceContext().getCallerController());
            TimeSmesh = cp.getInt("smesh") * 1000 * 60 * 60;
            devs.sort("controller", true);
            int controller = -1;
            for (DataRecord dev : devs) {
                try {

                    if (controller != dev.getInt("controller")) {
                        controller = dev.getInt("controller");
                        canals.add(new CanalMaster(controller, tableDecode, cp.getLong("timeOut")));
                    }
                    CanalMaster cm = canals.get(canals.size() - 1);
                    Socket socket = new Socket(InetAddress.getByName(dev.getString("IPaddr")), dev.getInt("port"));
                    cm.addDevice(socket);
                    canals.set(canals.size() - 1, cm);
                }
                catch (IOException ex) {
                    Log.CORE.info("Devices mount error " + ex.getMessage());
                }
            }
            for (CanalMaster cm : canals) {
                for (FwMasterDevice dev : cm.mdarray) {
                    try {
                        dev.setStepTime(cp.getLong("stepTime"));
                        dev.setStepGiveMe(cp.getLong("giveTime"));
                        dev.setStepLive(cp.getLong("liveTime"));
                        dev.gogo();
                        if (dev.isConnected()) {
                            if (FwUtil.FP_DEBUG) {
                                Log.CORE.info("Device is connected " + dev.myAddress());
                            }
                            if (FwUtil.FP_DEBUG) {
                                Log.CORE.info(Long.toString(dev.getStepTime()) + " " + Long.toString(dev.getStepGiveMe()) + " " + Long.toString(dev.getStepLive()));
                            }
                        }
                        else {
                            Log.CORE.info("Device is NOT connected " + dev.myAddress());
                        }
                    }
                    catch (Exception ex) {
                        Log.CORE.info("Devices error " + ex.getMessage());
                    }
                }
            }
        }
        catch (ContextException ex) {
            Log.CORE.info("Devices error " + ex.getMessage());
        }

        if (thReadRec == null) {
            thReadRec = new MultiReconect(this, thrManRec);
        }

        try {
            DataRecord sqlrec = getDeviceContext().getVariable("SQLProperties", getDeviceContext().getCallerController()).rec();
            yesSQL = sqlrec.getBoolean("yesSQL");
            if (!yesSQL) {
                super.connect();
                return;
            }
            stepSQL=sqlrec.getLong("stepSQL");
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
            errorSQL = false;
        }
        catch (ContextException | ClassNotFoundException | SQLException ex) {
            //yesSQL = false;
            Log.CORE.info(ex.getMessage());
//            throw new DeviceException(ex.getMessage());
        }
        if (thReadSQL == null) {
            thReadSQL = new MultiSQL(this, thrManSQL);
        }
        if (thReadMakeSQL == null) {
            thReadMakeSQL = new MakeSQL(this, thrManMakeSQL);
        }
        super.connect();
    }
    private long stepSQL;
    private Connection con = null;
    private Statement stmt = null;
    private Long MaxLenght;
    private Long TekPos;
    private Long LastPos;
    private String myDB;
    private String myDBH;
    private boolean yesSQL = false;
    private boolean errorSQL = true;

    private ThreadManager thrManSQL = new ThreadManager();
    private MultiSQL thReadSQL = null;

    private ThreadManager thrManMakeSQL = new ThreadManager();
    private MakeSQL thReadMakeSQL = null;

    private ThreadManager thrManRec = new ThreadManager();
    private MultiReconect thReadRec = null;

    ;

    @Override
    public void disconnect() throws DeviceException
    {
        super.disconnect(); //To change body of generated methods, choose Tools | Templates.
        if (canals == null) {
            return;
        }
        for (CanalMaster cm : canals) {
            for (FwMasterDevice dev : cm.mdarray) {
                dev.disconect();
            }
        }
        tableDecode = null;
        canals = null;

        if (yesSQL) {
            yesSQL = false;
            try {
                con.close();
                stmt.close();
            }
            catch (SQLException ex) {
                throw new DeviceException(ex);
            }
        }
    }

    @Override
    public void startSynchronization() throws DeviceException
    {
        super.startSynchronization(); //To change body of generated methods, choose Tools | Templates.
        if (canals == null) {
            return;
        }
//        Log.CORE.info("Начинаем startSynchronization");
        DataTable td_35h = new DataTable(getDeviceContext().getEventData("Event_35H").getDefinition().getFormat());
        DataTable td_36h = new DataTable(getDeviceContext().getEventData("Event_36H").getDefinition().getFormat());
        for (CanalMaster cm : canals) {
            cm.changeCanal();
            FwOneReg oreg;
            while ((oreg = cm.getHistory()) != null) {
                String name = reversdata.get(oreg.getReg().getKey());
                data.put(name, oreg);
                history.add(oreg);
            }
            cm.clearDatas();
            FwBaseMess mess;
            for (FwMasterDevice dev : cm.mdarray) {
                while ((mess = dev.readMessage()) != null) {
                    if (mess.getFunctionCode() == FwUtil.FP_CODE_35H) {
                        DataRecord dr = td_35h.addRecord();
                        dr.setValue("Controller", dev.getController());
                        dr.setValue("IPaddr", dev.myIP());
                        dr.setValue("port", dev.myPort());
                        FwKvit ms = (FwKvit) mess;
                        dr.setValue("file", ms.getNomFile());
                        dr.setValue("nomer", ms.getNomer());
                        dr.setValue("command", ms.getCmd());
                        dr.setValue("result", ms.getRezult());
                    }
                    if (mess.getFunctionCode() == FwUtil.FP_CODE_36H) {
                        DataRecord dr = td_36h.addRecord();
                        dr.setValue("Controller", dev.getController());
                        dr.setValue("IPaddr", dev.myIP());
                        dr.setValue("port", dev.myPort());
                        FwSetup ms = (FwSetup) mess;
                        dr.setValue("file", ms.getNomFile());
                        dr.setValue("nomer", ms.getNomer());
                        dr.setValue("command", ms.getCmd());
                        dr.setValue("lenght", ms.getLenght());
                        dr.setValue("buffer", ms.getBuffer());
                    }
                }
            }
        }
        if (td_35h.getRecordCount() > 0) {
//            Log.CORE.info("Event 35H");
            getDeviceContext().fireEvent("Event_35H", td_35h);
        }
        if (td_36h.getRecordCount() > 0) {
//            Log.CORE.info("Event 36H");
            getDeviceContext().fireEvent("Event_36H", td_36h);
        }
//        Log.CORE.info("Закончили startSynchronization");
    }

    @Override
    @SuppressWarnings("empty-statement")
    public void finishSynchronization() throws DeviceException, DisconnectionException
    {
//        Log.CORE.info("Начали finishSynchronization");
        ldF.regEventListner();
//        Log.CORE.info("Закончили finishSynchronization " + Integer.toString(rezult.length()));

    }
    private long TimeSmesh;

    @Override
    public DataTable readVariableValue(VariableDefinition vd, CallerController caller) throws ContextException, DeviceException, DisconnectionException
    {
        FwOneReg oreg = data.get(vd.getName());
        DataTable res = new DataTable(vd.getFormat());
        if (oreg == null) {
            res.addRecord(0);
            return res;
        }
        res.addRecord(oreg.getValue());
        return res;
    }

    @Override
    public List<EventDefinition> readEventDefinitions(DeviceEntities entities) throws ContextException, DeviceException, DisconnectionException
    {
        List<EventDefinition> ed = new LinkedList<>();
        TableFormat Format = new TableFormat(true);
        Format.addField(FieldFormat.create("<Controller><I><D=Номер канала>"));
        Format.addField(FieldFormat.create("<IPaddr><S><D=IP address устройства>"));
        Format.addField(FieldFormat.create("<port><I><D=Номер порта>"));
        Format.addField(FieldFormat.create("<file><I><D=Номер файла>"));
        Format.addField(FieldFormat.create("<nomer><I><D=Номер посылки>"));
        Format.addField(FieldFormat.create("<command><I><D=Код команды>"));
        Format.addField(FieldFormat.create("<result><I><D=Код результата>"));
        ed.add(new EventDefinition("Event_35H", Format, "Квитирование", ContextUtils.GROUP_DEFAULT));
        Format = new TableFormat(true);
        Format.addField(FieldFormat.create("<Controller><I><D=Номер канала>"));
        Format.addField(FieldFormat.create("<IPaddr><S><D=IP address устройства>"));
        Format.addField(FieldFormat.create("<port><I><D=Номер порта>"));
        Format.addField(FieldFormat.create("<file><I><D=Номер файла>"));
        Format.addField(FieldFormat.create("<nomer><I><D=Номер посылки>"));
        Format.addField(FieldFormat.create("<command><I><D=Код команды>"));
        Format.addField(FieldFormat.create("<lenght><I><D=Длина буфера>"));
        Format.addField(FieldFormat.create("<buffer><S><D=Строка от контроллера>"));
        ed.add(new EventDefinition("Event_36H", Format, "Настройка", ContextUtils.GROUP_DEFAULT));
        return ed;
    }

    @Override
    public List<FunctionDefinition> readFunctionDefinitions(DeviceEntities entities)
    {
        List res = new LinkedList();
        FieldFormat iff = FieldFormat.create("GetUPCMessage", FieldFormat.BOOLEAN_FIELD, "Состояние всех Устройств");
        TableFormat inputFormat = new TableFormat(1, 1, iff);
        TableFormat outputFormat = new TableFormat(true);
        outputFormat.addField(FieldFormat.create("<Controller><I><D=Номер канала>"));
        outputFormat.addField(FieldFormat.create("<IPaddr><S><D=IP address устройства>"));
        outputFormat.addField(FieldFormat.create("<port><I><D=Номер порта>"));
        outputFormat.addField(FieldFormat.create("<Connect><B><D=Наличие связи>"));
        outputFormat.addField(FieldFormat.create("<UPC><S><D=Состояние ИБП>"));
        outputFormat.addField(FieldFormat.create("<Status><S><D=Состояние Обмена>"));

        FunctionDefinition fd = new FunctionDefinition("GetUPCMessage", inputFormat, outputFormat, "Состояние всех Устройств", ContextUtils.GROUP_DEFAULT);
        res.add(fd);
//        iff = FieldFormat.create("SendMessage", FieldFormat.BOOLEAN_FIELD, "Состояние всех Устройств");
        inputFormat = new TableFormat(true);
        inputFormat.addField(FieldFormat.create("<Controller><I><D=Номер канала>"));
        inputFormat.addField(FieldFormat.create("<File><S><D=Имя файла>"));
        inputFormat.addField(FieldFormat.create("<Nom_File><I><D=Номер файла>"));
        outputFormat = new TableFormat(true);
        outputFormat.addField(FieldFormat.create("<Status><B><D=Результат запроса>"));
        outputFormat.addField(FieldFormat.create("<Text><S><D=Состояние запроса>"));
        fd = new FunctionDefinition("SendFile", inputFormat, outputFormat, "Передать файл в устройство", ContextUtils.GROUP_DEFAULT);
        res.add(fd);

        inputFormat = new TableFormat(true);
        inputFormat.addField(FieldFormat.create("<Controller><I><D=Номер канала>"));
        inputFormat.addField(FieldFormat.create("<Nom_File><I><D=Номер файла>"));
        outputFormat = new TableFormat(true);
        outputFormat.addField(FieldFormat.create("<Status><B><D=Результат запроса>"));
        outputFormat.addField(FieldFormat.create("<Text><S><D=Состояние запроса>"));
        fd = new FunctionDefinition("StartRecive", inputFormat, outputFormat, "Начать прием файла из устройства", ContextUtils.GROUP_DEFAULT);
        res.add(fd);
        inputFormat = new TableFormat(true);
        inputFormat.addField(FieldFormat.create("<Controller><I><D=Номер канала>"));
        inputFormat.addField(FieldFormat.create("<File><S><D=Имя файла>"));
        inputFormat.addField(FieldFormat.create("<Nom_File><I><D=Номер файла>"));
        outputFormat = new TableFormat(true);
        outputFormat.addField(FieldFormat.create("<Status><B><D=Результат запроса>"));
        outputFormat.addField(FieldFormat.create("<Text><S><D=Состояние запроса>"));
        fd = new FunctionDefinition("ReadFile", inputFormat, outputFormat, "Записать принятый файл", ContextUtils.GROUP_DEFAULT);
        res.add(fd);

        inputFormat = new TableFormat(true);
        inputFormat.addField(FieldFormat.create("<Controller><I><D=Номер канала>"));
        inputFormat.addField(FieldFormat.create("<Command><I><D=Код команды>"));
        inputFormat.addField(FieldFormat.create("<P1><I><D=Параметр1>"));
        inputFormat.addField(FieldFormat.create("<P2><I><D=Параметр2>"));
        inputFormat.addField(FieldFormat.create("<P3><I><D=Параметр3>"));
        inputFormat.addField(FieldFormat.create("<P4><I><D=Параметр4>"));
        inputFormat.addField(FieldFormat.create("<P5><I><D=Параметр5>"));
        inputFormat.addField(FieldFormat.create("<P6><I><D=Параметр6>"));
        inputFormat.addField(FieldFormat.create("<P7><I><D=Параметр7>"));
        outputFormat = new TableFormat(true);
        outputFormat.addField(FieldFormat.create("<Status><B><D=Результат запроса>"));
        outputFormat.addField(FieldFormat.create("<Text><S><D=Состояние запроса>"));
        fd = new FunctionDefinition("SendCtrl", inputFormat, outputFormat, "Изменить состояние устройства", ContextUtils.GROUP_DEFAULT);
        res.add(fd);
        return res;
    }

    @Override
    public DataTable executeFunction(FunctionDefinition fd, CallerController caller, DataTable parameters) throws ContextException, DeviceException, DisconnectionException
    {
        if (fd.getName().equals("GetUPCMessage")) {
            if (!parameters.rec().getBoolean("GetUPCMessage")) {
                return new DataTable(fd.getOutputFormat(), "Статус не запрашивался");
            }
            DataTable res = new DataTable(fd.getOutputFormat());
            for (CanalMaster cm : canals) {
                for (FwMasterDevice dev : cm.mdarray) {
                    DataRecord dr = res.addRecord();
                    dr.setValue("Controller", dev.getController());
                    dr.setValue("IPaddr", dev.myIP());
                    dr.setValue("port", dev.myPort());
                    dr.setValue("Connect", dev.isConnected());
                    dr.setValue("UPC", dev.getUPCMessage());
                    dr.setValue("Status", ldF.Status(dev.getController()));
                }
            }
            return res;
        }
        if (fd.getName().equals("SendCtrl")) {

            DataTable res = new DataTable(fd.getOutputFormat());
            for (CanalMaster cm : canals) {
                if (cm.getController() != parameters.rec().getInt("Controller")) {
                    continue;
                }
                FwMesCtrl outmess = new FwMesCtrl(parameters.rec().getInt("Controller"),
                        parameters.rec().getInt("Command"));
//                outmess.setLenght(parameters.rec().getInt("Lenght"));
//                byte[] b=parameters.rec().getString("Buffer").getBytes();
//                Log.CORE.info("Buffer="+Arrays.toString(b));
                outmess.setPar(0, parameters.rec().getInt("P1"));
                outmess.setPar(1, parameters.rec().getInt("P2"));
                outmess.setPar(2, parameters.rec().getInt("P3"));
                outmess.setPar(3, parameters.rec().getInt("P4"));
                outmess.setPar(4, parameters.rec().getInt("P5"));
                outmess.setPar(5, parameters.rec().getInt("P6"));
                outmess.setPar(6, parameters.rec().getInt("P7"));

                for (FwMasterDevice dev : cm.mdarray) {
                    dev.putMessage(outmess);
                }
                DataRecord dr = res.addRecord();
                dr.setValue("Status", true);
                dr.setValue("Text", "Сообщение передано");
                return res;
            }
            DataRecord dr = res.addRecord();
            dr.setValue("Status", false);
            dr.setValue("Text", "Сообщение не передано");
            return res;
        }
        if (fd.getName().equals("SendFile")) {

            DataTable res = new DataTable(fd.getOutputFormat());
            boolean rb;
            rb = ldF.addLoad(parameters.rec().getInt("Controller"), parameters.rec().getInt("Nom_File"), parameters.rec().getString("File"));
            DataRecord dr = res.addRecord();
            if (rb) {
                dr.setValue("Status", true);
                dr.setValue("Text", "Передача файла начата");
            }
            else {
                dr.setValue("Status", false);
                dr.setValue("Text", "Ошибка в процессе запуска операции");
            }
            return res;
        }

        if (fd.getName().equals("ReadFile")) {

            DataTable res = new DataTable(fd.getOutputFormat());
            boolean rb;
            rb = ldF.getFile(parameters.rec().getInt("Controller"), parameters.rec().getInt("Nom_File"), parameters.rec().getString("File"));
            DataRecord dr = res.addRecord();
            if (rb) {
                dr.setValue("Status", true);
                dr.setValue("Text", "Файл записан");
            }
            else {
                dr.setValue("Status", false);
                dr.setValue("Text", "Ошибка операции");

            }

            return res;
        }
        if (fd.getName().equals("StartRecive")) {

            DataTable res = new DataTable(fd.getOutputFormat());
            boolean rb;

            rb = ldF.startLoad(parameters.rec().getInt("Controller"), parameters.rec().getInt("Nom_File"));
            DataRecord dr = res.addRecord();
            if (rb) {
                dr.setValue("Status", true);
                dr.setValue("Text", "Прием файла начат");
            }
            else {
                dr.setValue("Status", false);
                dr.setValue("Text", "Ошибка в процессе запуска операции");
            }
            return res;
        }

        return super.executeFunction(fd, caller, parameters); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<VariableDefinition> readVariableDefinitions(DeviceEntities entities) throws ContextException, DeviceException, DisconnectionException
    {
        List res = new LinkedList();
        makeAllTables();
        DataTable regs = getDeviceContext().getVariable("registers", getDeviceContext().getCallerController());
        for (DataRecord reg : regs) {
            char type = ' ';
            switch (reg.getInt("format")) {
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
        if (tableDecode != null) {
            return;
        }
        try {
            Date now = new Date((new Date().getTime()) + TimeSmesh);
            history = new ConcurrentLinkedQueue<>();
            DataTable regs = getDeviceContext().getVariable("registers", getDeviceContext().getCallerController());
            tableDecode = new FwRegisters(FwUtil.VALUE_UIDS);
            data = new ConcurrentHashMap<>(FwUtil.VALUE_UIDS);
            reversdata = new ConcurrentHashMap<>(FwUtil.VALUE_UIDS);
            for (DataRecord reg : regs) {
                FwRegister fwreg = new FwRegister(reg.getInt("canal"), reg.getInt("type") == 0, reg.getInt("uId"), reg.getInt("format"), reg.getInt("lenght"));
                tableDecode.add(fwreg);
                FwOneReg oreg = new FwOneReg(now, fwreg);
                data.put(reg.getString("name"), oreg);
                reversdata.put(oreg.getReg().getKey(), reg.getString("name"));
            }
        }
        catch (ContextException ex) {
            Log.CORE.info("Registers not found" + ex.getMessage());
        }

    }
    private ConcurrentHashMap<String, FwOneReg> data = null;
    private ConcurrentHashMap<Long, String> reversdata = null;

    private ConcurrentLinkedQueue<FwOneReg> history = null;
    private FwRegisters tableDecode = null;
    public ArrayList<CanalMaster> canals = null;
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

    private static Map LenghtSelectionValues()
    {
        Map reg = new LinkedHashMap();
        reg.put(1, "Один байт");
        reg.put(2, "Два байта");
        reg.put(4, "Четыре байта");
        return reg;
    }

    static {
        VFT_CONNECTION_PROPERTIES = new TableFormat(1, 1);

        VFT_CONNECTION_PROPERTIES.addField(FieldFormat.create("<stepTime><L><A=500><D=Период опроса устройств>"));
        VFT_CONNECTION_PROPERTIES.addField(FieldFormat.create("<giveTime><L><A=10000><D=Интервал обновления информации о переменных>"));
        VFT_CONNECTION_PROPERTIES.addField(FieldFormat.create("<liveTime><L><A=10000><D=Интервал обновления отметки жизни>"));
        VFT_CONNECTION_PROPERTIES.addField(FieldFormat.create("<timeOut><L><A=20000><D=Стандартный тайм-аут ввода/вывода>"));
        VFT_CONNECTION_PROPERTIES.addField(FieldFormat.create("<smesh><I><A=9><D=Смещение в часах местного времени от сервера>"));

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
        ff = FieldFormat.create("<lenght><I><A=2><D=Длина в байтах>");
        ff.setSelectionValues(LenghtSelectionValues());
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
        VFT_SQL.addField(FieldFormat.create("<stepSQL><L><D=Интервал времени запросов SQL>"));
        

    }
    private ConcurrentLinkedQueue<StSQL> hSQL = new ConcurrentLinkedQueue<StSQL>();

    private class MakeSQL extends AggreGateThread
    {

        private OmronFinsDeviceDriver fd;
        ThreadManager threadManager = null;

        public MakeSQL(OmronFinsDeviceDriver fd, ThreadManager threadManager)
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
            while(!isInterrupted()) {
                
                try {
                    AggreGateThread.sleep(stepSQL);
                }
                catch (InterruptedException ex) {
                    return;
                }
                FwOneReg oreg;
                if (!yesSQL) {
                    while (history.poll() != null);
                    continue;
                }
                HashMap<String, FwOneReg> tHm = new HashMap<>(FwUtil.VALUE_UIDS);
                for (FwOneReg onereg : data.values()) {
                    String key = reversdata.get(onereg.getReg().getKey()) + Long.toString(onereg.getDate().getTime());
                    //Log.CORE.info("Ключ из data "+key);
                    tHm.put(key, onereg);
                }
                while ((oreg = history.poll()) != null) {
                    String key = reversdata.get(oreg.getReg().getKey()) + Long.toString(oreg.getDate().getTime());
                    tHm.put(key, oreg);
                }
                StringBuffer rezult = new StringBuffer();
//        Integer i=0;
//        Log.CORE.info("Начали ");

                for (FwOneReg onereg : tHm.values()) {
//            i++;
                    String vname = reversdata.get(onereg.getReg().getKey());
                    rezult.append("<" + vname + "=");

                    Object obj = onereg.getValue();
                    switch (obj.getClass().getName()) {
                        case "java.lang.Boolean":
                            rezult.append(((Boolean) obj ? "1" : "0"));
                            break;
                        case "java.lang.Long":
                            rezult.append(Long.toString((long) obj));
                            break;
                        case "java.lang.Integer":
                            rezult.append(Integer.toString((int) obj));
                            break;
                        default:
                            rezult.append(Float.toString((Float) obj));
                            break;
                    }
                    rezult.append("_" + Long.toString(onereg.getDate().getTime()) + ">");
                }
//        Log.CORE.info("Записали " + Integer.toString(rezult.length()));

                Timestamp timestamp = new Timestamp((new Date().getTime()) + TimeSmesh);
                StSQL mySQL = new StSQL(timestamp, rezult);
                hSQL.add(mySQL);

            }
            
        }

    }

    public class StSQL
    {

        private Timestamp timestamp;
        private StringBuffer var;

        StSQL(Timestamp timestamp, StringBuffer var)
        {
            this.timestamp = timestamp;
            this.var = var;
        }

        public Timestamp getTimestamp()
        {
            return timestamp;
        }

        public StringBuffer getVar()
        {
//            Log.CORE.info("Отдали "+Integer.toString(var.length()));
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
            do {
                try {
                    DataRecord cp = getDeviceContext().getVariable("connectionProperties", getDeviceContext().getCallerController()).rec();
                    DataTable devs = getDeviceContext().getVariable("devices", getDeviceContext().getCallerController());
                    devs.sort("controller", true);
                    for (DataRecord dev : devs) {
                        CanalMaster ccm = null;
                        for (CanalMaster cm : canals) {
                            if (cm.controller == dev.getInt("controller")) {
                                ccm = cm;
                                break;
                            }
                        }
                        if (ccm != null) {
                            boolean flag = false;
                            for (FwMasterDevice md : ccm.mdarray) {
                                if (md.myIP().equals(dev.getString("IPaddr")) && md.myPort() == dev.getInt("port")) {
                                    flag = true;
                                    break;
                                }
                            }
                            if (!flag) {
                                try {
                                    Socket socket = new Socket(InetAddress.getByName(dev.getString("IPaddr")), dev.getInt("port"));
                                    FwMasterDevice md = ccm.addDevice(socket);
                                    if (md != null) {
                                        md.setStepTime(cp.getLong("stepTime"));
                                        md.setStepGiveMe(cp.getLong("giveTime"));
                                        md.setStepLive(cp.getLong("liveTime"));
                                        md.gogo();
                                    }
                                }
                                catch (Exception ex) {
                                }
                            }
                        }
                    }
                    for (CanalMaster cm : canals) {
                        for (int idx = 0; idx < cm.mdarray.size(); idx++) {
                            FwMasterDevice dev = cm.mdarray.get(idx);
                            if (dev == null) {
                                continue;
                            }
                            //Log.CORE.info(" Проверяем " + dev.myAddress());
                            if (dev.isConnected()) {
                                continue;
                            }
                            cm.reconnectDevice(idx);
                            dev = cm.mdarray.get(idx);
                            if (dev == null) {
                                continue;
                            }
                            dev.setStepTime(cp.getLong("stepTime"));
                            dev.setStepGiveMe(cp.getLong("giveTime"));
                            dev.setStepLive(cp.getLong("liveTime"));
                            dev.gogo();
                            if (dev.isConnected()) {
                                if (FwUtil.FP_DEBUG) {
                                    Log.CORE.info("Device is connected " + dev.myAddress());
                                }
                                if (FwUtil.FP_DEBUG) {
                                    Log.CORE.info(Long.toString(dev.getStepTime()) + " " + Long.toString(dev.getStepGiveMe()) + " " + Long.toString(dev.getStepLive()));
                                }
                            }
                        }
                    }
                }
                catch (Exception ex) {
                    Log.CORE.info("Reconnect Exception " + ex.getMessage());
                }
                try {
                    AggreGateThread.sleep(1000L);
                }
                catch (InterruptedException ex) {
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
            do {
                if (!yesSQL) {
                    return;
                }
                try {
                    if (hSQL.size() > 10) {
                        Log.CORE.info("в очереди на запись запросов " + hSQL.size());
                    }
                    StSQL mySt;
                    if (errorSQL) {
                        ReconectSQL();
                        AggreGateThread.sleep(stepSQL);
                        continue;
                    }
                    while ((mySt = hSQL.poll()) != null) {
                        String rez;
                        if (LastPos > MaxLenght) {
                            rez = "UPDATE " + myDB + " SET tm='" + mySt.getTimestamp().toString() + "',var='" + mySt.getVar().toString() + "' WHERE id=" + TekPos.toString() + ";";
                            TekPos++;
                        }
                        else {
                            rez = "INSERT INTO " + myDB + "(id,tm,var) VALUES( " + TekPos.toString() + ",'" + mySt.getTimestamp().toString() + "','" + mySt.getVar().toString() + "')";
                            LastPos++;
                            TekPos++;
                        }
                        stmt.executeUpdate(rez);
                        if (TekPos > MaxLenght) {
                            TekPos = 0L;
                        }
                        rez = "UPDATE " + myDBH + " SET pos=" + TekPos.toString() + ", last=" + LastPos.toString() + " WHERE id=1";
                        stmt.executeUpdate(rez);
//                        Log.CORE.info("SQL sended " + mySt.getTimestamp().toString());

                    }

                    AggreGateThread.sleep(stepSQL);
                }
                catch (InterruptedException ex) {
                    //Log.CORE.info("stop driver ");
                    return;
                }
                catch (SQLException ex) {
                    Log.CORE.info("SqlException " + ex.getMessage());
                    ReconectSQL();
                }
            }
            while (!isInterrupted());
        }

        private void ReconectSQL()
        {
            errorSQL = true;
            try {
                DataRecord sqlrec = getDeviceContext().getVariable("SQLProperties", getDeviceContext().getCallerController()).rec();
                yesSQL = sqlrec.getBoolean("yesSQL");
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
                errorSQL = false;
            }
            catch (ContextException | ClassNotFoundException | SQLException ex) {
                Log.CORE.info(ex.getMessage());
            }
        }
    }

}
