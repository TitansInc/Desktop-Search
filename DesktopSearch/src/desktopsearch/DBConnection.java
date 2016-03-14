/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package desktopsearch;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author zubair
 */
public class DBConnection {

    
    private static String DBLocation=System.getProperty("user.home")+"/.DSearch/";
    private Connection connection;
    private Statement statement;
    
    public DBConnection(){
        MakeConnection();
    }
    
    public void MakeConnection(){
        
        try {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) { e.printStackTrace(); }
            
            connection=DriverManager.getConnection("jdbc:sqlite:"+DBLocation+"DSearch.db");
            connection.setAutoCommit(false);
            statement=connection.createStatement();
            CheckWhetherTableExists(statement);
            
        } catch (SQLException e) {    
            File file=new File(DBLocation);
            if(!file.exists())
            file.mkdir();
            MakeConnection();            
        }  
    
    } 
    
    public Statement getStatement(){
        return statement;
    }
     
    public Connection getConnection(){
        return connection;
    }
    
    private void CheckWhetherTableExists(Statement statement){
        try {
            String sql="SELECT * FROM FileInfo LIMIT 1;";
            statement.executeQuery(sql);
            
        } catch (SQLException ex) {
            String fileInfoTable="CREATE TABLE FileInfo("
                    + "FileLocation TEXT NOT NULL, "
                    + "FileName TEXT NOT NULL, "
                    + "Type VARCHAR(30), "
                    + "LastModified DATETIME,"
                    + "Size DOUBLE,"
                    + "PRIMARY KEY(FileLocation, FileName)); ";
           
            String IndexingInfoTable="CREATE TABLE IndexingInfo("
                    + "IndexID INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "IsFileWritingComplete INTEGER NOT NULL, "
                    + "IsIndexingComplete INTEGER NOT NULL, "
                    + "NoOfEntriesFinished LONG,"
                    + "StartTime DATETIME,"
                    + "EndTime DATETIME );";
            try {
                statement.executeUpdate(fileInfoTable);
                statement.executeUpdate(IndexingInfoTable);
                connection.commit();
            } catch (SQLException e) {e.printStackTrace();}
        }
    }
    
}
