package org.opencds.cqf.utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FilenameUtils;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.hl7.elm.r1.IncludeDef;
import org.hl7.elm.r1.ValueSetDef;
import org.hl7.fhir.instance.model.api.IAnyResource;

import org.opencds.cqf.library.GenericLibrarySourceProvider;
import org.opencds.cqf.terminology.ValueSetsProcessor;
import org.opencds.cqf.utilities.IOUtils.Encoding;

import ca.uhn.fhir.context.FhirContext;

public class ResourceUtils 
{
    public enum FhirVersion 
    { 
        DSTU3("dstu3"), R4("r4"); 

        private String string;     
        public String toString() 
        { 
            return this.string; 
        } 
    
        private FhirVersion(String string) 
        { 
            this.string = string; 
        }

        public static FhirVersion parse(String value) {
            switch (value) {
                case "dstu3": 
                    return DSTU3;
                case "r4":
                    return R4;
                default: 
                    throw new RuntimeException("Unable to parse FHIR version value:" + value);
            }
        }
    }

    public static String getId(String name, String version, boolean versioned) {
      return name.replaceAll("_", "-") + (versioned ? "-" + version.replaceAll("_", ".") : "");
    }

    public static void setIgId(String baseId, IAnyResource resource, Boolean includeVersion)
    {
      String version = includeVersion ? resource.getMeta().getVersionId() : "";
      setIgId(baseId, resource,  version);
    }
    
    public static void setIgId(String baseId, IAnyResource resource, String version)
    {
      String igId = "";
      String resourceName = resource.getClass().getSimpleName().toLowerCase();
      String versionId = (version == null || version.equals("")) ? "" : "-" + version;
      
      if (resource instanceof org.hl7.fhir.dstu3.model.Bundle || resource instanceof org.hl7.fhir.r4.model.Bundle) {
        igId = baseId + versionId + "-" + resourceName;        
      }
      else {
        igId = resourceName + "-" + baseId + versionId;

      }
      igId = igId.replace("_", "-");
      resource.setId(igId);
    }

    public static FhirContext getFhirContext(FhirVersion fhirVersion) {
      switch (fhirVersion) {
        case DSTU3:
          return FhirContext.forDstu3();
        case R4:
          return FhirContext.forR4();
        default:
          throw new IllegalArgumentException("Unknown FHIR version: " + fhirVersion);
      }
    }

    private static List<org.hl7.fhir.dstu3.model.RelatedArtifact> getStu3RelatedArtifacts(String pathToLibrary, FhirContext fhirContext) {
      Object mainLibrary = IOUtils.readResource(pathToLibrary, fhirContext);
      if (!(mainLibrary instanceof org.hl7.fhir.dstu3.model.Library)) {
        throw new IllegalArgumentException("pathToLibrary must be a path to a Library type Resource");
      }
      return ((org.hl7.fhir.dstu3.model.Library)mainLibrary).getRelatedArtifact();   
    }    

    private static List<org.hl7.fhir.r4.model.RelatedArtifact> getR4RelatedArtifacts(String pathToLibrary, FhirContext fhirContext) {
      Object mainLibrary = IOUtils.readResource(pathToLibrary, fhirContext);
      if (!(mainLibrary instanceof org.hl7.fhir.r4.model.Library)) {
        throw new IllegalArgumentException("pathToLibrary must be a path to a Library type Resource");
      }
      return ((org.hl7.fhir.r4.model.Library)mainLibrary).getRelatedArtifact();   
    }    

    public static Map<String, IAnyResource> getDepLibraryResources(String path, FhirContext fhirContext, Encoding encoding) {      
      Map<String, IAnyResource> dependencyLibraries = new HashMap<String, IAnyResource>();
      switch (fhirContext.getVersion().getVersion()) {
        case DSTU3:
            return getStu3DepLibraryResources(path, dependencyLibraries, fhirContext, encoding);
        case R4:
            return getR4DepLibraryResources(path, dependencyLibraries, fhirContext, encoding);
        default:
            throw new IllegalArgumentException("Unknown fhir version: " + fhirContext.getVersion().getVersion().getFhirVersionString());
      }
    }

    public static List<String> getDepLibraryPaths(String path, FhirContext fhirContext, Encoding encoding) {
      switch (fhirContext.getVersion().getVersion()) {
        case DSTU3:
            return getStu3DepLibraryPaths(path, fhirContext, encoding);
        case R4:
            return getR4DepLibraryPaths(path, fhirContext, encoding);
        default:
            throw new IllegalArgumentException("Unknown fhir version: " + fhirContext.getVersion().getVersion().getFhirVersionString());
      }
    }

    private static List<String> getStu3DepLibraryPaths(String path, FhirContext fhirContext, Encoding encoding) {
      List<String> paths = new ArrayList<String>();
      String directoryPath = FilenameUtils.getFullPath(path);
      List<org.hl7.fhir.dstu3.model.RelatedArtifact> relatedArtifacts = getStu3RelatedArtifacts(path, fhirContext);
      for (org.hl7.fhir.dstu3.model.RelatedArtifact relatedArtifact : relatedArtifacts) {
        String dependencyLibraryName = IOUtils.formatFileName(relatedArtifact.getResource().getReference().split("Library/")[1], encoding, fhirContext);
        String dependencyLibraryPath = FilenameUtils.concat(directoryPath, dependencyLibraryName);
        IOUtils.putInListIfAbsent(dependencyLibraryPath, paths);
      }
      return paths;
    }

    private static Map<String, IAnyResource> getStu3DepLibraryResources(String path, Map<String, IAnyResource> dependencyLibraries, FhirContext fhirContext, Encoding encoding) {      
      List<String> dependencyLibraryPaths = getStu3DepLibraryPaths(path, fhirContext, encoding);
      for (String dependencyLibraryPath : dependencyLibraryPaths) {
        Object resource = IOUtils.readResource(dependencyLibraryPath, fhirContext);
        if (resource instanceof org.hl7.fhir.dstu3.model.Library) {
          org.hl7.fhir.dstu3.model.Library library = (org.hl7.fhir.dstu3.model.Library)resource;
          dependencyLibraries.putIfAbsent(library.getId(), library);
        }
      }
      return dependencyLibraries;
    }

    private static List<String> getR4DepLibraryPaths(String path, FhirContext fhirContext, Encoding encoding) {
      List<String> paths = new ArrayList<String>();
      String directoryPath = FilenameUtils.getFullPath(path);
      List<org.hl7.fhir.r4.model.RelatedArtifact> relatedArtifacts = getR4RelatedArtifacts(path, fhirContext);
      for (org.hl7.fhir.r4.model.RelatedArtifact relatedArtifact : relatedArtifacts) {
        String dependencyLibraryName = IOUtils.formatFileName(relatedArtifact.getResource().split("Library/")[1], encoding, fhirContext);
        String dependencyLibraryPath = FilenameUtils.concat(directoryPath, dependencyLibraryName);
        IOUtils.putInListIfAbsent(dependencyLibraryPath, paths);
      }
      return paths;
    }

    private static Map<String, IAnyResource> getR4DepLibraryResources(String path, Map<String, IAnyResource> dependencyLibraries, FhirContext fhirContext, Encoding encoding) {      
      List<String> dependencyLibraryPaths = getR4DepLibraryPaths(path, fhirContext, encoding);
      for (String dependencyLibraryPath : dependencyLibraryPaths) {
        Object resource = IOUtils.readResource(dependencyLibraryPath, fhirContext);
        if (resource instanceof org.hl7.fhir.r4.model.Library) {
          org.hl7.fhir.r4.model.Library library = (org.hl7.fhir.r4.model.Library)resource;
          dependencyLibraries.putIfAbsent(library.getId(), library);
        }
      }
      return dependencyLibraries;
    }
    
    public static Map<String, IAnyResource> getDepValueSetResources(String cqlContentPath, String igPath, FhirContext fhirContext, boolean includeDependencies) throws Exception {
      Map<String, IAnyResource> valueSetResources = new HashMap<String, IAnyResource>();
      List<String> valueSetIDs = getDepValueSetIDs(cqlContentPath);
      HashSet<String> dependencies = new HashSet<>();
        
      for (String valueSetId : valueSetIDs) {
          ValueSetsProcessor.getCachedValueSets(fhirContext).entrySet().stream()
          .filter(entry -> entry.getKey().equals(valueSetId))
          .forEach(entry -> valueSetResources.putIfAbsent(entry.getKey(), entry.getValue()));
      }
      dependencies.addAll(valueSetIDs);

      if(includeDependencies) {
        List<String> dependencyCqlPaths = IOUtils.getDependencyCqlPaths(cqlContentPath);
        for (String path : dependencyCqlPaths) {
          Map<String, IAnyResource> dependencyValueSets = getDepValueSetResources(path, igPath, fhirContext, includeDependencies);
          dependencies.addAll(dependencyValueSets.keySet());
          for (Entry<String, IAnyResource> entry : dependencyValueSets.entrySet()) {
            valueSetResources.putIfAbsent(entry.getKey(), entry.getValue());
          }
        }
      }

      if (dependencies.size() != valueSetResources.size()) {
        String message = (dependencies.size() - valueSetResources.size()) + " missing ValueSets: \r\n";
        dependencies.removeAll(valueSetResources.keySet());
        for (String valueSetId : dependencies) {
          message += valueSetId + " MISSING \r\n";
        }   
        //System.out.println(message);
        throw new Exception(message);
      }
      return valueSetResources;
    }   

    public static ArrayList<String> getIncludedLibraryNames(String cqlContentPath) {
      ArrayList<String> includedLibraryNames = new ArrayList<String>();
      ArrayList<IncludeDef> includedDefs = getIncludedDefs(cqlContentPath);
      for (IncludeDef def : includedDefs) {
        //TODO: replace true with versioned variable
        IOUtils.putInListIfAbsent(getId(def.getPath(), def.getVersion(), true), includedLibraryNames);
      }
      return includedLibraryNames;
    }

    public static ArrayList<String> getDepValueSetIDs(String cqlContentPath) {
      ArrayList<String> includedValueSetIDs = new ArrayList<String>();
      ArrayList<ValueSetDef> valueSetDefs = getValueSetDefs(cqlContentPath);
      for (ValueSetDef def : valueSetDefs) {
        IOUtils.putInListIfAbsent(def.getId(), includedValueSetIDs);
      }
      return includedValueSetIDs;
    }

    public static ArrayList<IncludeDef> getIncludedDefs(String cqlContentPath) {
      ArrayList<IncludeDef> includedDefs = new ArrayList<IncludeDef>();
      org.hl7.elm.r1.Library elm;
      try {
        elm = getElmFromCql(cqlContentPath);
      } catch (Exception e) {
        System.out.println("error proccessing cql: ");
        System.out.println(e.getMessage());
        return includedDefs;
      }
      
      if (elm.getIncludes() != null && !elm.getIncludes().getDef().isEmpty()) {
        for (IncludeDef def : elm.getIncludes().getDef()) {
          includedDefs.add(def);
        }
      }
      return includedDefs;
    }

    public static ArrayList<ValueSetDef> getValueSetDefs(String cqlContentPath) {
      ArrayList<ValueSetDef> valueSetDefs = new ArrayList<ValueSetDef>();
      org.hl7.elm.r1.Library elm;
      try {
        elm = getElmFromCql(cqlContentPath);
      } catch (Exception e) {
        System.out.println("error translating cql: ");
        return valueSetDefs;
      }
      if (elm.getValueSets() != null && !elm.getValueSets().getDef().isEmpty()) {
        for (ValueSetDef def : elm.getValueSets().getDef()) {
          valueSetDefs.add(def);
        }
      }
      return valueSetDefs;
    }

    private static Map<String, org.hl7.elm.r1.Library> cachedElm = new HashMap<String, org.hl7.elm.r1.Library>();
    public static org.hl7.elm.r1.Library getElmFromCql(String cqlContentPath) {
      org.hl7.elm.r1.Library elm = cachedElm.get(cqlContentPath);
      if (elm != null) {
        return elm;
      }
      String cqlDirPath = IOUtils.getParentDirectoryPath(cqlContentPath);
      ModelManager modelManager = new ModelManager();
      GenericLibrarySourceProvider sourceProvider = new GenericLibrarySourceProvider(cqlDirPath);
      LibraryManager libraryManager = new LibraryManager(modelManager);
      libraryManager.getLibrarySourceLoader().registerProvider(sourceProvider);

      CqlTranslator translator = IOUtils.translate(cqlContentPath, modelManager, libraryManager);      
      elm = translator.toELM(); 
      cachedElm.put(cqlContentPath, elm);
      return elm; 
    }  

    public static Boolean safeAddResource(String path, Map<String, IAnyResource> resources, FhirContext fhirContext) {
      Boolean added = true;
      try {
          IAnyResource resource = IOUtils.readResource(path, fhirContext, true);
          if (resource != null) {
            resources.putIfAbsent(resource.getId(), resource);
          } else {
            added = false;
            LogUtils.putWarning(path, "Unable to add Resource: " + path);
          }
      }
      catch(Exception e) {
          added = false;
          LogUtils.putWarning(path, e.getMessage());
      }  
      return added;
  } 
}
