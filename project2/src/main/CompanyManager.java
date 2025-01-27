package main;

import main.interfaces.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CompanyManager implements ICompanyManager {
    PreparedStatement loginStatement;
    PreparedStatement importTaxRateStatement;
    PreparedStatement exportTaxRateStatement;
    PreparedStatement loadItemStatement;
    PreparedStatement loadContainerCheckItemStatement;
    PreparedStatement loadToShipStatement;
    PreparedStatement startSailingStatement;
    PreparedStatement unloadItemStatement;
    PreparedStatement setItemWaitForCheckingStatement;

    @Override
    public double getImportTaxRate(LogInfo logInfo, String city, String itemClass) {
        if (!login(logInfo)) return -1;
        try {
            if (importTaxRateStatement == null) {
                importTaxRateStatement = getConnection().prepareStatement("SELECT import_tax, price from item where type = ? and import_city = ? limit 1");
            }
            importTaxRateStatement.setString(1, itemClass);
            importTaxRateStatement.setString(2,city);
            ResultSet result = importTaxRateStatement.executeQuery();
            if (result.next()) {
                double price = result.getDouble("price");
                double tax = result.getDouble("import_tax");
                return tax/price;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }
    @Override
    public double getExportTaxRate(LogInfo logInfo, String city, String itemClass) {
        if (!login(logInfo)) return -1;
        try {
            if (exportTaxRateStatement == null) {
                exportTaxRateStatement = getConnection().prepareStatement("SELECT export_tax, price from item where type = ? and export_city = ? limit 1");
            }
            exportTaxRateStatement.setString(1, itemClass);
            exportTaxRateStatement.setString(2,city);
            ResultSet result = exportTaxRateStatement.executeQuery();
            if (result.next()) {
                double price = result.getDouble("price");
                double tax = result.getDouble("export_tax");
                return tax/price;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }
    @Override
    public boolean loadItemToContainer(LogInfo logInfo, String itemName, String containerCode) {
        if (!login(logInfo)) return false;
        try {
            if (!Util.getItemCompany(itemName).equals(Util.getCManagerCompany(logInfo.name()))) return false;
            if (!Util.itemExists(itemName,getConnection())) return false;
            if (Util.containerIsUsing(containerCode)) return false;
            if (Util.getItemState(itemName,getConnection()) != 4) return false;
            if (loadItemStatement == null) {
                String sql = "UPDATE item SET container_code = ? WHERE name = ?";
                loadItemStatement = getConnection().prepareStatement(sql);
            }
            loadItemStatement.setString(1, containerCode);
            loadItemStatement.setString(2, itemName);
            loadItemStatement.execute();
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public boolean loadContainerToShip(LogInfo logInfo, String shipName, String containerCode) {
        if (!login(logInfo)) return false;
        try {
            //ship is not ready
            if (Util.shipIsUsing(shipName)) return false;

            //no container for load
            if (loadContainerCheckItemStatement == null) {
                String sql = "SELECT * FROM item WHERE container_code = ? and state = ? and (ship_name = '' or ship_name is null)";
                loadContainerCheckItemStatement = getConnection().prepareStatement(sql);
            }
            loadContainerCheckItemStatement.setString(1, containerCode);
            loadContainerCheckItemStatement.setString(2, Util.intToState(4));
            ResultSet items = loadContainerCheckItemStatement.executeQuery();
            String itemName;
            if (!items.next()) return false;
            itemName = items.getString("name");
            if (!Util.getShipCompany(shipName).equals(Util.getCManagerCompany(logInfo.name()))) return false;
            if (!Util.getItemCompany(itemName).equals(Util.getCManagerCompany(logInfo.name()))) return false;

            if (loadToShipStatement == null) {
                String sql = "UPDATE item SET ship_name = ?, state = ?  WHERE name = ?";
                loadToShipStatement = getConnection().prepareStatement(sql);
            }
            loadToShipStatement.setString(1, shipName);
            loadToShipStatement.setString(2, Util.intToState(5));
            loadToShipStatement.setString(3, itemName);
            loadToShipStatement.execute();
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public boolean shipStartSailing(LogInfo logInfo, String shipName) {
        if (!login(logInfo)) return false;
        try {
            if (Util.shipIsUsing(shipName)) return false;
            if (!Util.getShipCompany(shipName).equals(Util.getCManagerCompany(logInfo.name()))) return false;
            if (startSailingStatement == null) {
                String sql = "SELECT * FROM item WHERE state = ? and ship_name = ?";
                startSailingStatement = getConnection().prepareStatement(sql);
            }
            startSailingStatement.setString(1, Util.intToState(5));
            startSailingStatement.setString(2 , shipName);
            ResultSet items = startSailingStatement.executeQuery();
            while (items.next()) {
                String itemName = items.getString("name");
                Util.setItemState(itemName, 6, getConnection());
            }
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public boolean unloadItem(LogInfo logInfo, String itemName) {
        if (!login(logInfo)) return false;
        try {
            if (!Util.itemExists(itemName,getConnection())) return false;
            if (!Util.getItemCompany(itemName).equals(Util.getCManagerCompany(logInfo.name()))) return false;
            if (unloadItemStatement == null) {
                String sql = "SELECT * FROM item WHERE name = ? and state = ?";
                unloadItemStatement = getConnection().prepareStatement(sql);
            }
            ResultSet resultSet;
            unloadItemStatement.setString(1, itemName);
            unloadItemStatement.setString(2 , Util.intToState(6));
            resultSet = unloadItemStatement.executeQuery();
            if (!resultSet.next()) return false;
            Util.setItemState(itemName, 7, getConnection());
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public boolean itemWaitForChecking(LogInfo logInfo, String itemName) {
        if (!login(logInfo)) return false;
        try {
            if (!Util.itemExists(itemName,getConnection())) return false;
            if (!Util.getItemCompany(itemName).equals(Util.getCManagerCompany(logInfo.name()))) return false;
            if (setItemWaitForCheckingStatement == null) {
                String sql = "SELECT * FROM item WHERE name = ? and state = ?";
                setItemWaitForCheckingStatement = getConnection().prepareStatement(sql);
            }
            ResultSet resultSet;
            setItemWaitForCheckingStatement.setString(1, itemName);
            setItemWaitForCheckingStatement.setString(2 , Util.intToState(7));
            resultSet = setItemWaitForCheckingStatement.executeQuery();
            if (!resultSet.next()) return false;
            Util.setItemState(itemName, 8, getConnection());

            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public boolean login(LogInfo logInfo) {
        if (logInfo.type() != LogInfo.StaffType.CompanyManager) {
            return false;
        }
        String password = Util.autoEncryptPassword(logInfo.password());
        try {
            if (loginStatement == null) {
                loginStatement = getConnection().prepareStatement("SELECT * FROM company_manager WHERE  name = ? AND password = ?");
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
        return ConnectionManager.getCompanyManagerConnection();
    }

}
