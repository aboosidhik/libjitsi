/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.device;

import java.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.libjitsi.*;

/**
 * Manages the list of active (currently plugged-in) capture/notify/playback
 * devices and manages user preferences between all known devices (previously
 * and actually plugged-in).
 *
 * @author Vincent Lucas
 */
public abstract class Devices
{
    /**
     * The audio system managing this device list.
     */
    private final AudioSystem audioSystem;

    /**
     * The selected active device.
     */
    private ExtendedCaptureDeviceInfo device = null;

    /**
     * The list of device ID/names saved by the configuration service and
     * previously saved given user preference order.
     */
    private final List<String> devicePreferences = new ArrayList<String>();

    /**
     * Initializes the device list management.
     *
     * @param audioSystem The audio system managing this device list.
     */
    public Devices(AudioSystem audioSystem)
    {
        this.audioSystem = audioSystem;
    }

    /**
     * Gets the selected active device.
     *
     * @param locator The string representation of the locator.
     * @param activeDevices The list of the active devices.
     *
     * @return The selected active device.
     */
    public ExtendedCaptureDeviceInfo getDevice(
            String locator,
            List<ExtendedCaptureDeviceInfo> activeDevices)
    {
        if (activeDevices != null)
        {
            String property = getPropDevice();
            loadDevicePreferences(locator, property);
            renameOldFashionedIdentifier(activeDevices);

            // Search if an active device is a new one (is not stored in the
            // preferences yet). If true, then active this device and set it as
            // default device (only for USB devices since the user has
            // deliberately plugged-in the device).
            for(ExtendedCaptureDeviceInfo activeDevice : activeDevices)
            {
                if(!devicePreferences.contains(activeDevice.getIdentifier()))
                {
                    // Adds the device in the preference list (to the end of the
                    // list, but the save device will push it to the top of
                    // active devices).
                    saveDevice(
                            locator,
                            property,
                            activeDevice,
                            activeDevices,
                            activeDevice.isSameTransportType("USB"));
                }
            }

            // Search if an active device match one of the previously configured
            // in the preferences.
            synchronized(devicePreferences)
            {
                for(String devicePreference : devicePreferences)
                {
                    for(ExtendedCaptureDeviceInfo activeDevice : activeDevices)
                    {
                        // If we have found the "preferred" device among active
                        // device.
                        if(devicePreference.equals(
                                activeDevice.getIdentifier()))
                        {
                            return activeDevice;
                        }
                        // If the "none" device is the "preferred" device among
                        // "active" device.
                        else if(devicePreference.equals(
                                    NoneAudioSystem.LOCATOR_PROTOCOL))
                        {
                            return null;
                        }
                    }
                }
            }
        }

        // Else if nothing was found, then returns null.
        return null;
    }

    /**
     * Returns the list of the active devices.
     *
     * @return The list of the active devices.
     */
    public abstract List<ExtendedCaptureDeviceInfo> getDevices();

    /**
     * Loads device name ordered with user's preference from the
     * <tt>ConfigurationService</tt>.
     *
     * @param locator The string representation of the locator.
     * @param property the name of the <tt>ConfigurationService</tt> property
     * which specifies the user's preference.
     */
    private void loadDevicePreferences(String locator, String property)
    {
        ConfigurationService cfg = LibJitsi.getConfigurationService();

        if (cfg != null)
        {
            String new_property
                = DeviceConfiguration.PROP_AUDIO_SYSTEM
                    + "."
                    + locator
                    + "."
                    + property
                    + "_list";

            String deviceIdentifiersString = cfg.getString(new_property);

            synchronized(devicePreferences)
            {
                if (deviceIdentifiersString != null)
                {
                    devicePreferences.clear();
                    // We must parse the string in order to load the device
                    // list.
                    String[] deviceIdentifiers = deviceIdentifiersString
                        .substring(2, deviceIdentifiersString.length() - 2)
                        .split("\", \"");
                    for(int i = 0; i < deviceIdentifiers.length; ++i)
                    {
                        devicePreferences.add(deviceIdentifiers[i]);
                    }
                }
                // Else, use the old property to load the last preferred device.
                // This whole "else" block may be removed in the future.
                else
                {
                    String old_property
                        = DeviceConfiguration.PROP_AUDIO_SYSTEM
                        + "."
                        + locator
                        + "."
                        + property;

                    deviceIdentifiersString = cfg.getString(old_property);

                    if (deviceIdentifiersString != null
                            && !NoneAudioSystem.LOCATOR_PROTOCOL
                                .equalsIgnoreCase(deviceIdentifiersString))
                    {
                        devicePreferences.clear();
                        devicePreferences.add(deviceIdentifiersString);
                    }
                }
            }
        }
    }

    /**
     * Saves the new selected device in top of the user preferences.
     *
     * @param locator The string representation of the locator.
     * @param property the name of the <tt>ConfigurationService</tt> property
     * into which the user's preference with respect to the specified
     * <tt>CaptureDeviceInfo</tt> is to be saved
     * @param selectedDevice The device selected by the user.
     * @param activeDevices The list of the active devices.
     * @param isSelected True if the device is the selected one.
     */
    private void saveDevice(
            String locator,
            String property,
            ExtendedCaptureDeviceInfo device,
            List<ExtendedCaptureDeviceInfo> activeDevices,
            boolean isSelected)
    {
        String selectedDeviceIdentifier = NoneAudioSystem.LOCATOR_PROTOCOL;
        if(device != null)
        {
            selectedDeviceIdentifier = device.getIdentifier();
        }

        // Sorts the user preferences to put the selected device on top.
        addToDevicePreferences(
                locator,
                activeDevices,
                selectedDeviceIdentifier,
                isSelected);

        // Saves the user preferences.
        writeDevicePreferences(locator, property);
    }

    /**
     * Selects the active device.
     *
     * @param locator The string representation of the locator.
     * @param device The selected active device.
     * @param save Flag set to true in order to save this choice in the
     * configuration. False otherwise.
     * @param activeDevices The list of the active devices.
     */
    public void setDevice(
            String locator,
            ExtendedCaptureDeviceInfo device,
            boolean save,
            List<ExtendedCaptureDeviceInfo> activeDevices)
    {
        // Checks if there is a change.
        if ((device == null) || !device.equals(this.device))
        {
            ExtendedCaptureDeviceInfo oldValue = this.device;

            // Saves the new selected device in top of the user preferences.
            if (save)
            {
                saveDevice(
                        locator,
                        getPropDevice(),
                        device,
                        activeDevices,
                        true);
            }
            this.device = device;

            audioSystem.propertyChange(getPropDevice(), oldValue, this.device);
        }
    }

    /**
     * Sets the list of the active devices.
     *
     * @param activeDevices The list of the active devices.
     */
    public abstract void setActiveDevices(
            List<ExtendedCaptureDeviceInfo> activeDevices);

    /**
     * Returns the property of the capture devices.
     *
     * @return The property of the capture devices.
     */
    protected abstract String getPropDevice();

    /**
     * Adds a new device in the preferences (at the first active position if the
     * isSelected argument is true).
     *
     * @param locator The string representation of the locator.
     * @param activeDevices The list of the active devices.
     * @param newsDeviceIdentifier The identifier of the device to add int first
     * active position of the preferences.
     * @param isSelected True if the device is the selected one.
     */
    private void addToDevicePreferences(
            String locator,
            List<ExtendedCaptureDeviceInfo> activeDevices,
            String newDeviceIdentifier,
            boolean isSelected)
    {
        int i;
        int j;

        synchronized(devicePreferences)
        {
            devicePreferences.remove(newDeviceIdentifier);
            if(isSelected)
            {
                // Searches for the first active device.
                for(i = 0; i < devicePreferences.size(); ++i)
                {
                    // Checks if this element is an active device.
                    for(j = 0; j < activeDevices.size(); ++j)
                    {
                        if(devicePreferences.get(i).equals(
                                    activeDevices.get(j).getIdentifier())
                                || devicePreferences.get(i).equals(
                                    NoneAudioSystem.LOCATOR_PROTOCOL))
                        {
                            // The first active device is found.
                            devicePreferences.add(i, newDeviceIdentifier);
                            // The device is added, stop the loops and quit.
                            return;
                        }
                    }
                }
            }
            // If there is no active devices or the device is not selected, then
            // set the new device to the end of the device preference list.
            devicePreferences.add(newDeviceIdentifier);
        }
    }

    /**
     * Renames the old fashioned identifier (name only), into new fashioned one
     * (UID, or name + transport type).
     *
     * @param activeDevices The list of the active devices.
     */
    private void renameOldFashionedIdentifier(
            List<ExtendedCaptureDeviceInfo> activeDevices)
    {
        String name;
        String id;
        int nameIndex;
        int idIndex;
        // Renames the old fashioned device identifier for all active devices.
        for(int i = 0; i < activeDevices.size(); ++i)
        {
            name = activeDevices.get(i).getName();
            id = activeDevices.get(i).getIdentifier();
            // We can only switch to the new fashioned notation, only if the OS
            // api give us a unique identifier (different from the device name).
            if(!name.equals(id))
            {
                synchronized(devicePreferences)
                {
                    do
                    {
                        nameIndex = devicePreferences.indexOf(
                                activeDevices.get(i).getName());
                        idIndex = devicePreferences.indexOf(
                                activeDevices.get(i).getIdentifier());
                        // If there is one old fashioned identifier.
                        if(nameIndex != -1)
                        {
                            // If the corresponding new fashioned identifier
                            // does not exists, then renames the old one into
                            // the new one.
                            if(idIndex == -1)
                            {
                                devicePreferences.set(nameIndex,
                                        activeDevices.get(i).getIdentifier());
                            }
                            // Else removes the duplicate.
                            else
                            {
                                devicePreferences.remove(nameIndex);
                            }
                        }
                    }
                    while(nameIndex != -1);
                }
            }
        }
    }

    /**
     * Saves the device preferences and write it to the configuration file.
     *
     * @param locator The string representation of the locator.
     * @param property the name of the <tt>ConfigurationService</tt> property
     */
    private void writeDevicePreferences(String locator, String property)
    {
        ConfigurationService cfg = LibJitsi.getConfigurationService();

        if (cfg != null)
        {
            property
                = DeviceConfiguration.PROP_AUDIO_SYSTEM
                    + "." + locator
                    + "." + property
                    + "_list";

            StringBuilder value = new StringBuilder("[\"");

            synchronized(devicePreferences)
            {
                if(devicePreferences.size() > 0)
                {
                    value.append(devicePreferences.get(0));
                    for(int i = 1; i < devicePreferences.size(); ++i)
                    {
                        value.append("\", \"").append(devicePreferences.get(i));
                    }
                }
            }
            value.append("\"]");

            cfg.setProperty(property, value.toString());
        }
    }
}
