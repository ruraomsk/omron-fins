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
import com.tibbo.aggregate.common.context.FunctionDefinition;
import com.tibbo.aggregate.common.datatable.DataRecord;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.datatable.FieldFormat;
import com.tibbo.aggregate.common.datatable.TableFormat;
import com.tibbo.aggregate.common.device.DeviceException;
import com.tibbo.aggregate.common.device.DisconnectionException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import ru.list.ruraomsk.fwlib.FwBaseMess;
import ru.list.ruraomsk.fwlib.FwKvit;
import ru.list.ruraomsk.fwlib.FwMasterDevice;
import ru.list.ruraomsk.fwlib.FwMesCtrl;
import ru.list.ruraomsk.fwlib.FwOneReg;
import ru.list.ruraomsk.fwlib.FwSetup;
import ru.list.ruraomsk.fwlib.FwUtil;
import ruraomsk.list.ru.strongsql.ParamSQL;
import ruraomsk.list.ru.strongsql.SetValue;

/**
 *
 * @author Yury Rusinov <ruraomsl@list.ru at Automatics E>
 */
public class DriverHelper {

    public static List<FunctionDefinition> makerFunctionDefinitions() {
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

        inputFormat = new TableFormat(1, 1);
        inputFormat.addField(FieldFormat.create("<name><S><D=Имя переменной>"));
        inputFormat.addField(FieldFormat.create("<from><D><D=Начало периода>"));
        inputFormat.addField(FieldFormat.create("<to><D><D=Конец периода>"));
        outputFormat = new TableFormat(true);
        outputFormat.addField(FieldFormat.create("<series><S><D=Имя серии>"));
        outputFormat.addField(FieldFormat.create("<x><D><D=Время>"));
        outputFormat.addField(FieldFormat.create("<y><F><D=Значение>"));
        fd = new FunctionDefinition("history", inputFormat, outputFormat, "История переменной", ContextUtils.GROUP_DEFAULT);
        res.add(fd);

        return res;

    }

    public static DataTable executeFunction(OmronFinsDeviceDriver aThis, FunctionDefinition fd, CallerController caller, DataTable parameters) {
        if (fd.getName().equals("GetUPCMessage")) {
            if (!parameters.rec().getBoolean("GetUPCMessage")) {
                return new DataTable(fd.getOutputFormat(), "Статус не запрашивался");
            }
            DataTable res = new DataTable(fd.getOutputFormat());
            for (CanalMaster cm : aThis.canals) {
                for (FwMasterDevice dev : cm.mdarray) {
                    DataRecord dr = res.addRecord();
                    dr.setValue("Controller", dev.getController());
                    dr.setValue("IPaddr", dev.myIP());
                    dr.setValue("port", dev.myPort());
                    dr.setValue("Connect", dev.isConnected());
                    dr.setValue("UPC", dev.getUPCMessage());
                    dr.setValue("Status", aThis.ldF.Status(dev.getController()));
                }
            }
            return res;
        }
        if (fd.getName().equalsIgnoreCase("history")) {
            String svar = parameters.rec().getString("name");
            Timestamp dfrom = new Timestamp(parameters.rec().getDate("from").getTime());
            Timestamp dto = new Timestamp(parameters.rec().getDate("to").getTime());
            FwOneReg oreg = aThis.data.get(svar);
            ArrayList<SetValue> asv = aThis.sqlseek.seekData(dfrom, dto, oreg.getReg().getKey());
            DataTable result = new DataTable(fd.getOutputFormat());
            for (SetValue sv : asv) {
                if (sv.getTime() == 0L) {
                    continue;
                }
                DataRecord rec = result.addRecord();
                rec.setValue("series", svar);
                rec.setValue("x", new Timestamp(sv.getTime()));
                rec.setValue("y", sv.getFloatValue());
            }
            return result;
        }
        if (fd.getName().equals("SendCtrl")) {

            DataTable res = new DataTable(fd.getOutputFormat());
            for (CanalMaster cm : aThis.canals) {
                if (cm.getController() != parameters.rec().getInt("Controller")) {
                    continue;
                }
                FwMesCtrl outmess = new FwMesCtrl(parameters.rec().getInt("Controller"),
                        parameters.rec().getInt("Command"));
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
            rb = aThis.ldF.addLoad(parameters.rec().getInt("Controller"), parameters.rec().getInt("Nom_File"), parameters.rec().getString("File"));
            DataRecord dr = res.addRecord();
            if (rb) {
                dr.setValue("Status", true);
                dr.setValue("Text", "Передача файла начата");
            } else {
                dr.setValue("Status", false);
                dr.setValue("Text", "Ошибка в процессе запуска операции");
            }
            return res;
        }

        if (fd.getName().equals("ReadFile")) {

            DataTable res = new DataTable(fd.getOutputFormat());
            boolean rb;
            rb = aThis.ldF.getFile(parameters.rec().getInt("Controller"), parameters.rec().getInt("Nom_File"), parameters.rec().getString("File"));
            DataRecord dr = res.addRecord();
            if (rb) {
                dr.setValue("Status", true);
                dr.setValue("Text", "Файл записан");
            } else {
                dr.setValue("Status", false);
                dr.setValue("Text", "Ошибка операции");

            }

            return res;
        }
        if (fd.getName().equals("StartRecive")) {

            DataTable res = new DataTable(fd.getOutputFormat());
            boolean rb;

            rb = aThis.ldF.startLoad(parameters.rec().getInt("Controller"), parameters.rec().getInt("Nom_File"));
            DataRecord dr = res.addRecord();
            if (rb) {
                dr.setValue("Status", true);
                dr.setValue("Text", "Прием файла начат");
            } else {
                dr.setValue("Status", false);
                dr.setValue("Text", "Ошибка в процессе запуска операции");
            }
            return res;
        }
        Log.CORE.error("Нет обработчика функции "+fd.getName());
        return null;
    }
    public static ParamSQL setParamVlr(DataTable SQLProp){
        ParamSQL param=new ParamSQL();
        DataRecord rec=SQLProp.rec();
        param.JDBCDriver=rec.getString("JDBCDriver");
        param.url=rec.getString("url");
        param.user=rec.getString("user");
        param.password=rec.getString("password");
        param.myDB=rec.getString("setups");
        return param;
    } 

    public static void makeFireEvent(OmronFinsDeviceDriver aThis){
        DataTable td_35h = new DataTable(aThis.getDeviceContext().getEventData("Event_35H").getDefinition().getFormat());
        DataTable td_36h = new DataTable(aThis.getDeviceContext().getEventData("Event_36H").getDefinition().getFormat());
        for (CanalMaster cm : aThis.canals) {
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
            aThis.getDeviceContext().fireEvent("Event_35H", td_35h);
        }
        if (td_36h.getRecordCount() > 0) {
            aThis.getDeviceContext().fireEvent("Event_36H", td_36h);
        }
        
    }
    public static ParamSQL setParamData(DataTable SQLProp){
        ParamSQL param=new ParamSQL();
        DataRecord rec=SQLProp.rec();
        param.JDBCDriver=rec.getString("JDBCDriver");
        param.url=rec.getString("url");
        param.user=rec.getString("user");
        param.password=rec.getString("password");
        param.myDB=rec.getString("datas");
        return param;
    } 

}
