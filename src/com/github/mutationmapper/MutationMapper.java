/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.mutationmapper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import net.minidev.json.parser.ParseException;




/**
 *
 * @author david
 */
public class MutationMapper extends Application implements Initializable{
    
    @FXML 
    MenuBar menuBar;
    @FXML
    Menu fileMenu;
    @FXML
    Menu helpMenu;
    @FXML
    ChoiceBox speciesChoiceBox;
    @FXML
    TextField geneTextField;
    @FXML
    TextField cdsTextField;
    @FXML
    TextField sequenceTextField;
    @FXML
    TextField positionTextField;
    @FXML
    TextField mutationTextField;
    @FXML
    Label progressLabel;
    @FXML
    ProgressBar progressBar;
    @FXML
    Button runButton;
    
    //Result display window
    FXMLLoader tableLoader;
    Pane tablePane;
    Scene tableScene;
    Stage tableStage;
    MutationMapperResultViewController resultView;
    
    final static EnsemblRest rest = new EnsemblRest();
    
    @Override
    public void start(final Stage primaryStage) {
        try{
            AnchorPane page;
            if (System.getProperty("os.name").equals("Mac OS X")){
                page = (AnchorPane) FXMLLoader.load(
                        com.github.mutationmapper.MutationMapper.class.
                                getResource("MutationMapper.fxml"));  
            }else{
                page = (AnchorPane) FXMLLoader.load(
                        com.github.mutationmapper.MutationMapper.class.
                                getResource("MutationMapper.fxml"));
            }
            Scene scene = new Scene(page);
            primaryStage.setScene(scene);
            primaryStage.setTitle("MutationMapper");
            primaryStage.setResizable(false);
            primaryStage.show();
            //primaryStage.getIcons().add(new Image(this.getClass().
            //        getResourceAsStream("icon.png")));
            primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
               @Override
               public void handle(WindowEvent e) {
                  Platform.exit();
                  System.exit(0);
               }
            });
            
        } catch (Exception ex) {
            Logger.getLogger(MutationMapper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        tableLoader = new FXMLLoader(getClass().
                                       getResource("MutationMapperResultView.fxml"));
        Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    geneTextField.requestFocus();
                }
            }
        );
        
        cdsTextField.textProperty().addListener(new ChangeListener<String>(){
            @Override
            public void changed(ObservableValue<? extends String> observable,
                    final String oldValue, final String newValue ){
                //newValue = newValue.trim();
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        sequenceTextField.setDisable(!newValue.isEmpty());
                        positionTextField.setDisable(!newValue.isEmpty());
                    }
                });
                
            }
        });
        positionTextField.textProperty().addListener(new ChangeListener<String>(){
            @Override
            public void changed(ObservableValue<? extends String> observable,
                    final String oldValue, final String newValue ){
                //newValue = newValue.trim();
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if (! newValue.isEmpty()){ 
                            cdsTextField.setDisable(true);
                        }else{
                            cdsTextField.setDisable(!sequenceTextField.getText().isEmpty());
                        }
                    }
                });
                
            }
        });
        
        sequenceTextField.textProperty().addListener(new ChangeListener<String>(){
            @Override
            public void changed(ObservableValue<? extends String> observable,
                    final String oldValue, final String newValue ){
                //newValue = newValue.trim();
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if (! newValue.isEmpty()){ 
                            cdsTextField.setDisable(true);
                        }else{
                            cdsTextField.setDisable(!positionTextField.getText().isEmpty());
                        }
                    }
                });
                
            }
        });
        
        runButton.setDefaultButton(true);
        runButton.setOnAction(new EventHandler<ActionEvent>(){
           @Override
           public void handle(ActionEvent actionEvent){
                mapMutation();
            }
       });
        
        getAvailableSpecies();
        
    }
    
    private void mapMutation() {
        final Task<List<MutationMapperResult>> mapperTask;
        final String gene = geneTextField.getText();
        if (gene.isEmpty()){
            return;
        }
        final String species = (String) speciesChoiceBox.getSelectionModel().getSelectedItem();
        if (species.isEmpty()){
            return;
        }
        final String cdsCoordinate = cdsTextField.getText();
        
        if (!cdsCoordinate.isEmpty()){
            if (mutationTextField.getText().isEmpty()){
                mapperTask = 
                        new Task<List<MutationMapperResult>>(){
                    @Override
                    protected List<MutationMapperResult> call() throws ParseException, MalformedURLException, IOException, InterruptedException {
                        /*returns arraylist of comma separated strings for gene symbol, gene ID,
                          transcript ID, chromosome, genomic coordinate, genome
                        */
                        List<MutationMapperResult> results = new ArrayList<>();
                        ArrayList<HashMap<String, String>> gCoords = rest.codingToGenomic(species, 
                                gene, Integer.parseInt(cdsCoordinate));
                        if (gCoords == null){
                            return null;
                        }
                        if (gCoords.isEmpty()){
                            return null;
                        }
                        for (HashMap<String, String> g: gCoords){
                            MutationMapperResult result = new MutationMapperResult();
                            result.setGeneSymbol(g.get("symbol"));
                            result.setGeneId(g.get("gene"));
                            result.setGenome(g.get("assembly"));
                            result.setTranscript(g.get("transcript"));
                            result.setCdsCoordinate(Integer.parseInt(cdsCoordinate));
                            result.setChromosome(g.get("chromosome"));
                            result.setCoordinate(Integer.parseInt(g.get("coordinate")));
                            results.add(result);
                        }
                        return results;
                    }
                };
            }else{
                mapperTask = new Task<List<MutationMapperResult>>(){
                    @Override
                    protected List<MutationMapperResult> call(){
                        List<MutationMapperResult> results = new ArrayList<>();
                        MutationMapperResult result = new MutationMapperResult();
                        //TO DO!
                        return results;
                    }
                };
            }
        }else{
            mapperTask = new Task<List<MutationMapperResult>>(){
                    @Override
                    protected List<MutationMapperResult> call(){
                        List<MutationMapperResult> results = new ArrayList<>();
                        MutationMapperResult result = new MutationMapperResult();
                        //TO DO!
                        return results;
                    }
                };
        }
        mapperTask.setOnSucceeded(new EventHandler<WorkerStateEvent>(){
            @Override
            public void handle (WorkerStateEvent e){
                setRunning(false);
                progressLabel.textProperty().unbind();
                progressBar.progressProperty().unbind();
                progressBar.progressProperty().set(0);
                runButton.setText("Run");
                runButton.setDefaultButton(true);
                ArrayList<MutationMapperResult> results = 
                        (ArrayList<MutationMapperResult>) e.getSource().getValue();
                if (results == null){
                    return;
                }
                try{
                    if (tablePane == null){
                        tablePane = (Pane) tableLoader.load();
                    }
                    if (resultView == null){
                        resultView = 
                            (MutationMapperResultViewController) tableLoader.getController();
                    }
                    if (tableScene == null){
                        tableScene = new Scene(tablePane);
                        tableStage = new Stage();
                        tableStage.setScene(tableScene);
                        tableStage.initModality(Modality.NONE);
                    }
                    resultView.displayData(results);
                    tableStage.setTitle("MutationMapper Results");
                    //tableStage.getIcons().add(new Image(this.getClass()
                    //        .getResourceAsStream("icon.png")));
                    if (!tableStage.isShowing()){
                        tableStage.show();
                    }else{
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                tableStage.requestFocus();
                            }
                        });
                    }
                }catch(IOException ex){
                    //TO DO - show error dialog
                    ex.printStackTrace();
                }

                
            }

        });
        
        new Thread(mapperTask).start();
        
        runButton.setDefaultButton(false);
        runButton.setCancelButton(true);
        runButton.setText("Cancel");
        
        
        
        
        
    }
    
    
    private void getAvailableSpecies(){
        final Task<List<String>> getSpeciesTask = new Task<List<String>>(){
            @Override
            protected List<String> call() 
                    throws ParseException, MalformedURLException, IOException, InterruptedException{
                System.out.println("Getting species.");
                List<String> species = rest.getAvailableSpecies();
                return species;
            }
        };
        
        getSpeciesTask.setOnSucceeded(new EventHandler<WorkerStateEvent>(){
            @Override
            public void handle (WorkerStateEvent e){
                List<String> species = (List<String>) e.getSource().getValue();
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        speciesChoiceBox.getItems().clear();
                        speciesChoiceBox.getItems().addAll(species);
                        speciesChoiceBox.getSelectionModel().selectFirst();
                    }
                });
            }
        });
        getSpeciesTask.setOnCancelled(null);
        getSpeciesTask.setOnFailed(new EventHandler<WorkerStateEvent>(){
            @Override
            public void handle (WorkerStateEvent e){
                //TO DO - ERROR DIALOG
                e.getSource().getException().printStackTrace();
            }
        });
        new Thread(getSpeciesTask).start();
    }
    
    private void canRun(boolean can){
        runButton.setDisable(!can);
    }
    
    private void setRunning(boolean running){
        canRun(!running);
        geneTextField.setDisable(running);
        sequenceTextField.setDisable(running);
        cdsTextField.setDisable(running);
        positionTextField.setDisable(running);
        mutationTextField.setDisable(running);
        speciesChoiceBox.setDisable(running);
    }
                
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
