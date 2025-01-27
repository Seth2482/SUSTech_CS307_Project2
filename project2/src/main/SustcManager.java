package main;

import main.interfaces.*;

import java.sql.*;

public class SustcManager implements ISustcManager {
    PreparedStatement loginStatement;
    PreparedStatement itemInfoStatement;
    PreparedStatement shipInfoStatement;
    PreparedStatement itemOfShipStatement;
    PreparedStatement containerInfoStatement;
    PreparedStatement itemOfContainerStatement;
    PreparedStatement staffInfoStatement;

    @Override
    public int getCompanyCount(LogInfo logInfo) {
        if (!login(logInfo)) {
            return -1;
        }
        try {
            Statement statement = getConnection().createStatement();
            ResultSet result = statement.executeQuery("SELECT count(*) as company_count FROM company");
            result.next();
            return result.getInt("company_count");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getCityCount(LogInfo logInfo) {
        if (!login(logInfo)) return -1;
        try {
            Statement statement = getConnection().createStatement();
            ResultSet result = statement.executeQuery("SELECT count(*) as city_count FROM (select name from city union select name from port_city) as cities");
            result.next();
            return result.getInt("city_count");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getCourierCount(LogInfo logInfo) {
        if (!login(logInfo)) {
            return -1;
        }
        try {
            Statement statement = getConnection().createStatement();
            ResultSet result = statement.executeQuery("SELECT count(*) as courier_count FROM courier");
            result.next();
            return result.getInt("courier_count");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getShipCount(LogInfo logInfo) {
        if (!login(logInfo)) {
            return -1;
        }
        try {
            Statement statement = getConnection().createStatement();
            ResultSet result = statement.executeQuery("SELECT count(*) as ship_count FROM ship");
            result.next();
            return result.getInt("ship_count");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ItemInfo getItemInfo(LogInfo logInfo, String s) {
        if (!login(logInfo)) {
            return null;
        }
        try {
            if (itemInfoStatement == null) {
                itemInfoStatement = getConnection().prepareStatement("SELECT * FROM item where item.name = ?");
            }
            itemInfoStatement.setString(1, s);
            ResultSet queryResult = itemInfoStatement.executeQuery();
            if (!queryResult.next()) {
                return null;
            }
            ItemState itemState = Util.stateTextToObject(queryResult.getString("state"));

            ItemInfo.RetrievalDeliveryInfo retrieval = new ItemInfo.RetrievalDeliveryInfo(queryResult.getString("from_city_name"), queryResult.getString("retrieval_courier"));
            ItemInfo.RetrievalDeliveryInfo delivery = new ItemInfo.RetrievalDeliveryInfo(queryResult.getString("to_city_name"), queryResult.getString("delivery_courier"));
            ItemInfo.ImportExportInfo $export, $import;
            /*if (queryResult.getString("export_officer") != null)
                $export = new ItemInfo.ImportExportInfo(queryResult.getString("export_city"), queryResult.getString("export_officer"), queryResult.getDouble("export_tax"));
            else $export = null;

            if (queryResult.getString("import_officer") != null)
                $import = new ItemInfo.ImportExportInfo(queryResult.getString("import_city"), queryResult.getString("import_officer"), queryResult.getDouble("import_tax"));
            else $import = null;*/

            $export = new ItemInfo.ImportExportInfo(queryResult.getString("export_city"), queryResult.getString("export_officer"), queryResult.getDouble("export_tax"));
            $import = new ItemInfo.ImportExportInfo(queryResult.getString("import_city"), queryResult.getString("import_officer"), queryResult.getDouble("import_tax"));

            ItemInfo result = new ItemInfo(queryResult.getString("name"), queryResult.getString("type"), queryResult.getDouble("price"), itemState, retrieval, delivery, $import, $export);
            return result;
        } catch (
                SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public ShipInfo getShipInfo(LogInfo logInfo, String s) {
        if (!login(logInfo)) {
            return null;
        }
        try {
            if (shipInfoStatement == null) {
                shipInfoStatement = getConnection().prepareStatement("SELECT * from ship where ship.name = ?");
            }
            shipInfoStatement.setString(1, s);
            ResultSet queryResult = shipInfoStatement.executeQuery();

            if (!queryResult.next()) return null;

            // judge whether the ship is sailing
            boolean isSailing;
            if (itemOfShipStatement == null) {
                itemOfShipStatement = getConnection().prepareStatement("SELECT count(*) as count FROM item where item.ship_name = ? AND item.state = 'Shipping'");
            }
            itemOfShipStatement.setString(1, s);
            ResultSet ships = itemOfShipStatement.executeQuery();
            ships.next();
            isSailing = ships.getInt("count") > 0;
            return new ShipInfo(queryResult.getString("name"), queryResult.getString("company_name"), isSailing);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ContainerInfo getContainerInfo(LogInfo logInfo, String s) {
        if (!login(logInfo)) {
            return null;
        }

        try {
            if (containerInfoStatement == null) {
                containerInfoStatement = getConnection().prepareStatement("SELECT * FROM container where container.code = ?");
            }
            containerInfoStatement.setString(1, s);
            ResultSet queryResult = containerInfoStatement.executeQuery();

            if (!queryResult.next()) return null;

            // check whether it is in use
            if (itemOfContainerStatement == null) {
                itemOfContainerStatement = getConnection().prepareStatement("SELECT count(*) from item where item.container_code = ? AND item.state != 'Finish'");
            }
            itemOfContainerStatement.setString(1, s);
            ResultSet itemQueryResult = itemOfContainerStatement.executeQuery();
            itemQueryResult.next();
            boolean inUse = itemQueryResult.getInt("count") > 0;

            // get its type
            ContainerInfo.Type type;
            switch (queryResult.getString("type")) {
                case "Dry Container":
                    type = ContainerInfo.Type.Dry;
                    break;
                case "Flat Rack Container":
                    type = ContainerInfo.Type.FlatRack;
                    break;
                case "ISO Tank Container":
                    type = ContainerInfo.Type.ISOTank;
                    break;
                case "Open Top Container":
                    type = ContainerInfo.Type.OpenTop;
                    break;
                case "Reefer Container":
                    type = ContainerInfo.Type.Reefer;
                    break;
                default:
                    type = null;

            }

            ContainerInfo containerInfo = new ContainerInfo(type, queryResult.getString("code"), inUse);

            return containerInfo;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public StaffInfo getStaffInfo(LogInfo logInfo, String s) {
        if (!login(logInfo)) {
            return null;
        }
        try {
            if (staffInfoStatement == null) {
                staffInfoStatement = getConnection().prepareStatement(
                        "SELECT " +
                                "name," +
                                "phone_number," +
                                "gender," +
                                "age," +
                                "company_name," +
                                "city_name, " +
                                "'Courier' as role, " +
                                "password " +
                                "FROM courier where courier.name = ? " +
                        "union SELECT name," +
                                "phone_number," +
                                "gender," +
                                "age," +
                                "company_name, null as city_name, " +
                                "'CompanyManager' as role, " +
                                "password    FROM company_manager where company_manager.name = ?" +
                        "union SELECT " +
                                "name," +
                                "phone_number," +
                                "gender," +
                                "age, " +
                                "null as company_name, " +
                                "null as city_name, " +
                                "'SustcManager' as role, " +
                                "password " +
                                "from sustc_manager where sustc_manager.name = ?" +
                        "union SELECT " +
                                "name," +
                                "phone_number," +
                                "gender," +
                                "age, " +
                                "null as company_name, " +
                                "port_city_name as city_name, " +
                                "'SeaportOfficer' as role, " +
                                "password " +
                                "from officer where officer.name = ?");
            }
            staffInfoStatement.setString(1, s);
            staffInfoStatement.setString(2, s);
            staffInfoStatement.setString(3, s);
            staffInfoStatement.setString(4, s);

            ResultSet queryResult = staffInfoStatement.executeQuery();
            if (!queryResult.next()) return null;
            boolean isFemale = queryResult.getString("gender").equals("female");
            LogInfo basicInfo = new LogInfo(queryResult.getString("name"), LogInfo.StaffType.valueOf(queryResult.getString("role")), queryResult.getString("password"));
            return new StaffInfo(basicInfo, queryResult.getString("company_name"), queryResult.getString("city_name"), isFemale, queryResult.getInt("age"), queryResult.getString("phone_number"));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean login(LogInfo logInfo) {
        if (logInfo.type() != LogInfo.StaffType.SustcManager) {
            return false;
        }
        String password = Util.autoEncryptPassword(logInfo.password());
        try {
            if (loginStatement == null) {
                loginStatement = getConnection().prepareStatement("SELECT * FROM sustc_manager WHERE  name = ? AND password = ?");
            }
            loginStatement.setString(1, logInfo.name());
            loginStatement.setString(2, password);
            ResultSet result = loginStatement.executeQuery();
            return result.next();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Connection getConnection() {
        return ConnectionManager.getSustcManagerConnection();
    }
}
