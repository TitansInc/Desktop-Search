/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package desktopsearch;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

/**
 *
 * @author zubair
 */
public class IndexedSearch{

    Connection connection;
    Statement statement;
    
    IndexedSearch(Connection connection, Statement statement){
        this.connection=connection;
        this.statement=statement;
    }
    
    public HashMap<Integer,String[]> FindInFilenames(String FileName){
        HashMap<Integer, String[]> searchedData=new HashMap<Integer, String[]>();
        try{
        
            String Query="SELECT * from FileInfo WHERE FileName like '%"+FileName.replace("'","''")+"%';";
            ResultSet result=statement.executeQuery(Query);
            
            int count=1;
            
            while (result.next()) {
                String[] data = new String[5];
                data[0] = result.getString("FileLocation");
                data[1] = result.getString("FileName");
                data[2] = result.getString("Type");
                data[3] = result.getString("LastModified");
                data[4] = result.getString("Size");
                searchedData.put(count++, data);
            }

        }catch(Exception e){e.printStackTrace();}        
        return searchedData;        
    }
    
    
}
