package com.pahimar.ee3.util;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.pahimar.ee3.api.exchange.EnergyValue;
import com.pahimar.ee3.exchange.EnergyValueStackMapping;
import com.pahimar.ee3.exchange.OreStack;
import com.pahimar.ee3.exchange.WrappedStack;
import com.pahimar.ee3.knowledge.TransmutationKnowledge;
import com.pahimar.ee3.reference.Reference;
import com.pahimar.ee3.util.serialize.*;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.TreeMap;

public class SerializationHelper {

    public static final Type ENERGY_VALUE_MAP_TYPE = new TypeToken<Map<WrappedStack, EnergyValue>>(){}.getType();
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .enableComplexMapKeySerialization()
            .registerTypeAdapter(ItemStack.class, new ItemStackSerializer())
            .registerTypeAdapter(OreStack.class, new OreStackSerializer())
            .registerTypeAdapter(FluidStack.class, new FluidStackSerializer())
            .registerTypeAdapter(WrappedStack.class, new WrappedStackSerializer())
            .registerTypeAdapter(ENERGY_VALUE_MAP_TYPE, new EnergyValueMapSerializer())
            .create();

    private static File instanceDataDirectory;
    private static File instancePlayerDataDirectory;

    /**
     * Returns a File reference to the mod specific directory in the data directory
     *
     * @return
     */
    public static File getInstanceDataDirectory()
    {
        return instanceDataDirectory;
    }

    /**
     * Returns a File reference to the mod specific directory in the playerdata directory
     *
     * @return
     */
    public static File getInstancePlayerDataDirectory()
    {
        return instancePlayerDataDirectory;
    }

    /**
     * Creates (if one does not exist already) and initializes a mod specific File reference inside of the current world's playerdata directory
     */
    public static void initModDataDirectories()
    {
        instanceDataDirectory = new File(FMLCommonHandler.instance().getMinecraftServerInstance().getEntityWorld().getSaveHandler().getWorldDirectory(), "data" + File.separator + Reference.LOWERCASE_MOD_ID);
        instanceDataDirectory.mkdirs();

        instancePlayerDataDirectory = new File(FMLCommonHandler.instance().getMinecraftServerInstance().getEntityWorld().getSaveHandler().getWorldDirectory(), "playerdata" + File.separator + Reference.LOWERCASE_MOD_ID);
        instancePlayerDataDirectory.mkdirs();
    }

    public static TransmutationKnowledge readTransmutationKnowledgeFromFile(File directory, String fileName)
    {
        if (!directory.exists())
        {
            directory.mkdirs();
        }

        return TransmutationKnowledge.readFromFile(new File(directory, fileName));
    }

    public static void writeTransmutationKnowledgeToFile(File directory, String fileName, TransmutationKnowledge transmutationKnowledge)
    {
        writeTransmutationKnowledgeToFile(directory, fileName, transmutationKnowledge, false);
    }

    public static void writeTransmutationKnowledgeToFile(File directory, String fileName, TransmutationKnowledge transmutationKnowledge, boolean verboseLogging)
    {
        if (directory != null && fileName != null)
        {
            if (!directory.exists())
            {
                directory.mkdirs();
            }

            if (transmutationKnowledge == null)
            {
                transmutationKnowledge = new TransmutationKnowledge();
            }

            try
            {
                File file1 = new File(directory, fileName + ".tmp");
                File file2 = new File(directory, fileName);
                TransmutationKnowledge.writeToFile(file1, transmutationKnowledge);

                if (file2.exists())
                {
                    file2.delete();
                }

                file1.renameTo(file2);

                if (verboseLogging)
                {
                    LogHelper.info("Successfully saved TransmutationKnowledge to file: {}", file2.getAbsolutePath());
                }
            }
            catch (Exception exception)
            {
                exception.printStackTrace();
                LogHelper.error("Failed to save TransmutationKnowledge to file: {}{}", directory.getAbsolutePath(), fileName);
            }
        }
    }

    public static Map<WrappedStack, EnergyValue> readEnergyValueStackMapFromJsonFile(String fileName)
    {
        File energyValuesDataDirectory = new File(FMLCommonHandler.instance().getMinecraftServerInstance().getEntityWorld().getSaveHandler().getWorldDirectory(), "data" + File.separator + Reference.LOWERCASE_MOD_ID + File.separator + "energyvalues");
        return readEnergyValueStackMapFromJsonFile(new File(energyValuesDataDirectory, fileName));
    }

    public static Map<WrappedStack, EnergyValue> readEnergyValueStackMapFromJsonFile(File jsonFile)
    {
        Map<WrappedStack, EnergyValue> energyValueStackMap = new TreeMap<WrappedStack, EnergyValue>();
        JsonReader jsonReader;

        try
        {
            jsonReader = new JsonReader(new FileReader(jsonFile));
            jsonReader.beginArray();
            while (jsonReader.hasNext())
            {
                EnergyValueStackMapping energyValueStackMapping = EnergyValueStackMapping.jsonSerializer.fromJson(jsonReader, EnergyValueStackMapping.class);
                if (energyValueStackMapping != null)
                {
                    energyValueStackMap.put(energyValueStackMapping.wrappedStack, energyValueStackMapping.energyValue);
                }
            }
            jsonReader.endArray();
            jsonReader.close();
        }
        catch (FileNotFoundException ignored)
        {
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return energyValueStackMap;
    }

    public static void writeEnergyValueStackMapToJsonFile(String fileName, Map<WrappedStack, EnergyValue> energyValueMap)
    {
        File energyValuesDataDirectory = new File(FMLCommonHandler.instance().getMinecraftServerInstance().getEntityWorld().getSaveHandler().getWorldDirectory(), "data" + File.separator + Reference.LOWERCASE_MOD_ID + File.separator + "energyvalues");
        writeEnergyValueStackMapToJsonFile(new File(energyValuesDataDirectory, fileName), energyValueMap);
    }

    public static void writeEnergyValueStackMapToJsonFile(File jsonFile, Map<WrappedStack, EnergyValue> energyValueMap)
    {
        JsonWriter jsonWriter;

        try
        {
            jsonWriter = new JsonWriter(new FileWriter(jsonFile));
            jsonWriter.setIndent("    ");
            jsonWriter.beginArray();
            for (WrappedStack wrappedStack : energyValueMap.keySet())
            {
                if (wrappedStack != null && wrappedStack.getWrappedObject() != null)
                {
                    EnergyValueStackMapping.jsonSerializer.toJson(new EnergyValueStackMapping(wrappedStack, energyValueMap.get(wrappedStack)), EnergyValueStackMapping.class, jsonWriter);
                }
            }

            jsonWriter.endArray();
            jsonWriter.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
