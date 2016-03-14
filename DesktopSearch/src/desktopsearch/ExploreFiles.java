/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package desktopsearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author zubair
 */
public class ExploreFiles implements Runnable {

    private static final Integer NO = 0;
    private static final Integer YES = 1;
    private static final Integer FileWritingFinishedIndicator =0;
    private static final Integer IndexingFinishedIndicator=1;
    private static final Map<String, Integer> Months;
    static
    {
        Months = new HashMap<String, Integer>();
        Months.put("Jan",0);
        Months.put("Feb",1);
        Months.put("Mar",2);
        Months.put("Apr",3);
        Months.put("May",4);
        Months.put("Jun",5);
        Months.put("Jul",6);
        Months.put("Aug",7);
        Months.put("Sep",8);
        Months.put("Oct",9); 
        Months.put("Nov",10);
        Months.put("Dec",11);
    }
    private Connection connection;
    private Statement statement;
    private String Location;
    private Integer IsWritingToFileFinished = NO;
    private Integer IsIndexingComplete = NO;
    private PrintWriter output;
    
    public ExploreFiles(Connection connection, Statement statement) throws FileNotFoundException {

        this.Location = System.getProperty("user.home");
        this.connection=connection;
        this.statement = statement;
        
    }
    
    
    @Override
    public void run() {
        try {

            Long NumOfFilesIndexed = CheckForUnfinishedIndexing();
            if (IsIndexingComplete.equals(NO)) {
                if (IsWritingToFileFinished.equals(NO)) {
                    
                    this.output = new PrintWriter(new FileOutputStream(Location + "/.DSearch/FilesList.sql"), true);
                    RecursivelyExplore(new File(Location));
                    IsWritingToFileFinished=YES;
                    UpdateIndexingProgress(FileWritingFinishedIndicator);
                    String SQL="SELECT NoOfEntriesFinished FROM IndexingInfo WHERE IndexID IN (SELECT max(IndexID) AS MaxID FROM IndexingInfo); ";
                    ResultSet resultOfQuery=statement.executeQuery(SQL);
                    WriteToDB(resultOfQuery.getInt("NoOfEntriesFinished"));
                    
                } else {
                    String SQL="SELECT NoOfEntriesFinished FROM IndexingInfo WHERE IndexID IN (SELECT max(IndexID) AS MaxID FROM IndexingInfo); ";
                    ResultSet resultOfQuery=statement.executeQuery(SQL);
                    WriteToDB(resultOfQuery.getInt("NoOfEntriesFinished"));
                }

            } else {               
              // Just do nothing for now  
              // UpdateChangesSinceLastShutdown(getLastSystemShutdown());
            }
           
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        StartFileSystemSensor();
    }

    private void StartFileSystemSensor(){
        
        try {
            Path path = Paths.get(System.getProperty("user.home"));
            new WatchDir(path, true,connection,statement).processEvents();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
//  //There is no way you can find you the deledted files
//  // Although you can find files created or updated from different users or partitions, 
//  // I don't think it is really necessary to incur that heavy cost for such a small feature?
//    private void UpdateChangesSinceLastShutdown(Integer NumOfDays) throws IOException{
//
//        Process proc = Runtime.getRuntime().exec("find "+System.getProperty("user.home")+" -mtime -2");
//        BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
//        String rawData,Query;
//        File file;
//        while((rawData=input.readLine())!=null){
//            try{
//            file=new File(rawData);
//            if(!file.exists()){
//             //File Always existst  
//            }
//            else{
//            }
//   
//            }catch(Exception e){e.printStackTrace(); }
//        }
//    }
//    
//    private Integer getLastSystemShutdown() throws IOException {
//
//        Process proc = Runtime.getRuntime().exec("last -x shutdown");
//        BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
//        String rawData = input.readLine();
//        String[] ProcessedData = rawData.split(" ");
//        Calendar calendar = Calendar.getInstance();
//        calendar.set(calendar.get(Calendar.YEAR), Months.get(ProcessedData[6]), Integer.valueOf(ProcessedData[7]));
//        Calendar currentTime = Calendar.getInstance();
//        Integer NumOfDays = 0;
//        while (calendar.getTimeInMillis() < currentTime.getTimeInMillis()) {
//            NumOfDays = NumOfDays + 1;
//            calendar.add(Calendar.DATE, 1);
//        }
//        return NumOfDays;
//    }
    
    
    private void UpdateIndexingProgress(Integer UpdateAttribute) {
        
        try {
            
            String Query = "UPDATE IndexingInfo SET ";
            if (UpdateAttribute.equals(FileWritingFinishedIndicator)) {
                Query = Query + "IsFileWritingComplete=1 WHERE IndexID IN (SELECT max(IndexID) AS MaxID FROM IndexingInfo);";
            } else if (UpdateAttribute.equals(IndexingFinishedIndicator)) {
                Query = Query + "IsIndexingComplete=1 WHERE IndexID IN (SELECT max(IndexID) AS MaxID FROM IndexingInfo) ;";
            }
            statement.executeUpdate(Query);
            connection.commit();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    
    private Long CheckForUnfinishedIndexing() throws SQLException {
        
        String Query = "SELECT count(*) AS LinesFinished FROM IndexingInfo;";
        ResultSet resultOfQuery = statement.executeQuery(Query);
        int NumOfRows = resultOfQuery.getInt("LinesFinished");
        
        if (NumOfRows == 0) {
            Query = "INSERT INTO IndexingInfo(IsFileWritingComplete, IsIndexingComplete, NoOfEntriesFinished,StartTime)"
                    + "VALUES (0,0,0,CURRENT_TIMESTAMP)";
            statement.executeUpdate(Query);
            connection.commit();
            return new Long(0);
        } else {

            Query = "SELECT * FROM IndexingInfo WHERE IndexID IN (SELECT max(IndexID) AS MaxID FROM IndexingInfo);";
            resultOfQuery = statement.executeQuery(Query);
            IsIndexingComplete = resultOfQuery.getInt("IsIndexingComplete");
            IsWritingToFileFinished = resultOfQuery.getInt("IsFileWritingComplete");
            return new Long(resultOfQuery.getLong("NoOfEntriesFinished"));
            
        }
    }

    
    private void WriteToDB(Integer LinesFinished){
        try {
            BufferedReader buffer=new BufferedReader(new FileReader(Location+"/.DSearch/FilesList.sql"));
            int count=0;    
            String SQL=null;
            
            while((SQL=buffer.readLine())!=null){
                
                if(count<LinesFinished){
                    count++;
                    continue;
                }
                statement.addBatch(SQL);  
                count++;
                if (count % 1000 == 0) {
                    try {
                        
                        statement.executeBatch();
                        SQL = "UPDATE IndexingInfo SET NoOfEntriesFinished=" + count + " WHERE IndexID IN (SELECT max(IndexID) AS MaxID FROM IndexingInfo)";
                        statement.executeUpdate(SQL);
                        connection.commit();
                        
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            SQL = "UPDATE IndexingInfo SET NoOfEntriesFinished=" + count + ",EndTime=CURRENT_TIMESTAMP WHERE IndexID IN (SELECT max(IndexID) AS MaxID FROM IndexingInfo)";
            statement.addBatch(SQL);
            statement.executeBatch();
            connection.commit();
            UpdateIndexingProgress(IndexingFinishedIndicator);
            
        } catch (Exception ex) {
                ex.printStackTrace();
        }
        
    }
    
    
    private void RecursivelyExplore(File Directory) {
 
        String[] List = Directory.list();
        String AbsPath = Directory.getAbsolutePath() + "/";
        String SQLQuery = null;
        ArrayList<String> DirList = new ArrayList();
        
        String FormattedPath=Directory.getPath();
        if (FormattedPath.contains("'")) {
            FormattedPath = FormattedPath.replace("'", "''");
        }


        try {
            for (int i = 0; i < List.length; i++) {
                File path = new File(AbsPath + List[i]);
                Path Location = Paths.get(path.getAbsolutePath());

                FileTime LastModifiedTime = Files.getLastModifiedTime(Location);
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(LastModifiedTime.toMillis());

                try {
                        
                    if (path.isFile()) {
                        
                        String Type = FilenameUtils.getExtension(Directory.getPath() + "/" + List[i]);
                        
                        if (Type.endsWith("~") || Type.equals("")) {
                            Type = "Text";
                        }

                        if(List[i].contains("'")){
                            List[i]=List[i].replace("'", "''");
                            System.out.println(List[i]);
                        }
                        
                        SQLQuery = "INSERT INTO FileInfo VALUES('" + Directory.getPath() + "','" + List[i] + "','" + Type + "','" + calendar.getTime() + "'," + (path.length()/1024) + ");";
                        
                    } else {
                        
                        if(List[i].contains("'")){
                            List[i]=List[i].replace("'", "''");
                            System.out.println(List[i]);
                        }
                        
                        if (!Files.isSymbolicLink(Location)) {
                            long size = FileUtils.sizeOfDirectory(path);
                            SQLQuery = "INSERT INTO FileInfo VALUES('" + Directory.getPath() + "','" + List[i] + "','FOLDER','" + calendar.getTime() + "'," + size + ");";
                        } else {

                            SQLQuery = "INSERT INTO FileInfo VALUES('" + Directory.getPath() + "','" + List[i] + "','LINK','" + calendar.getTime() + "',0);";
                        }

                        DirList.add(List[i]);
                    }
                    
                   
                    
                    output.println(SQLQuery);
                } catch (Exception exp) {
                    exp.printStackTrace();
                };

            }
            
            for (int i = 0; i < DirList.size(); i++) {
                File subDir = new File(AbsPath + DirList.get(i));
                Path path = Paths.get(AbsPath + DirList.get(i));
                if (subDir.canRead() && !Files.isSymbolicLink(path)) {
                    RecursivelyExplore(subDir);
            }

        }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
