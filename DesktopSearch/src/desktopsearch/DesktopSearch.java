/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package desktopsearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author zubaeyr
 */
public class DesktopSearch {

    /**
     * @param args the command line arguments
     */
    public DesktopSearch() throws FileNotFoundException, IOException {
        

        while(true){
        Process proc = Runtime.getRuntime().exec("w");
        BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String rawData;
        
            while((rawData=input.readLine())!=null){
                System.out.println(rawData);
            }
            
        }
        
        
//        
//        DBConnection connector = new DBConnection();
//        ExploreFiles explorer = new ExploreFiles(connector.getConnection(), connector.getStatement());
//        Thread explorerThread = new Thread(explorer);
//        explorerThread.start();
//        
//        
//        IndexedSearch indX=new IndexedSearch(connector.getConnection(), connector.getStatement());
//        HashMap<Integer, String[]> result=indX.FindInFilenames("single");
//        System.out.println(result.size()+" Records Found :");
//        for(Integer key : result.keySet()){
//            String array[]=result.get(key);
//            System.out.println(array[1]+"\t"+array[0]);
//            
//        }
//        System.exit(0);
    }

    public static void main(String[] args) throws IOException {

        DesktopSearch DSearch = new DesktopSearch();

    }

}
