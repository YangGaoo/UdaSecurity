package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.ImageService;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private SecurityService securityService;
    private Sensor sensor;
    private Sensor secondSensor;

    @Mock
    private ImageService imageService;

    @Mock
    private SecurityRepository securityRepository;

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);
        sensor = new Sensor("SensorDoor", SensorType.DOOR);
        secondSensor = new Sensor("SensorWindow", SensorType.WINDOW);
    }

    //TEST 1
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names={"ARMED_HOME", "ARMED_AWAY"})
    void changeSensorActivationStatusTest_alarmArmed_and_sensorActivated_thenReturn_PendingAlarmStatus(ArmingStatus armingStatus) {
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        when(securityService.getArmingStatus()).thenReturn(armingStatus);
        securityService.changeSensorActivationStatus(sensor, true);
        ArgumentCaptor<AlarmStatus> argumentCaptor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMost(1)).setAlarmStatus(argumentCaptor.capture());
        assertEquals(AlarmStatus.PENDING_ALARM, argumentCaptor.getValue());
    }

    //TEST 2
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names={"ARMED_HOME", "ARMED_AWAY"})
    void changeSensorActivationStatusTest_alarmArmed_and_sensorActivated_with_pendingStatus_thenReturn_Alarm(ArmingStatus armingStatus) {
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityService.getArmingStatus()).thenReturn(armingStatus);
        securityService.changeSensorActivationStatus(sensor, true);
        ArgumentCaptor<AlarmStatus> argumentCaptor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMost(1)).setAlarmStatus(argumentCaptor.capture());
        assertEquals(AlarmStatus.ALARM, argumentCaptor.getValue());
    }

    //TEST 3
    @Test
    void changeSensorActivationStatusTest_inactivatedSensors_with_pendingAlarm_thenReturn_NoAlarm() {
        sensor.setActive(true);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor,false);
        Mockito.verify(securityRepository, Mockito.times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
        //verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //TEST 4
    @Test
    void changeSensorActivationStatusTest_alarm_activated_sensorStatus_changed_thenReturn_Not_Affected_AlarmStatus() {
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor,false);
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }

    //TEST 5
    @Test
    void changeSensorActivationStatusTest_sensorActivated_with_pendingAlarm_thenReturn_Alarm() {
        sensor.setActive(true);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        ArgumentCaptor<AlarmStatus> argumentCaptor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository).setAlarmStatus(argumentCaptor.capture());
        assertEquals(AlarmStatus.ALARM, argumentCaptor.getValue());
    }

    //TEST 6
    @Test
        void changeSensorActivationStatusTest_sensor_deactivated_while_inactive_thenReturn_noChanges_to_AlarmStatus () {
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, times(0)).setAlarmStatus(any(AlarmStatus.class));
    }

    //TEST 7
    @Test
    void imageTest_cat_detected_while_system_Armed_thenReturn_Alarm () {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        ArgumentCaptor<AlarmStatus> argumentCaptor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository).setAlarmStatus(argumentCaptor.capture());
        assertEquals(AlarmStatus.ALARM, argumentCaptor.getValue());
    }

    //TEST 8
    @Test
    void imageTest_cat_not_detected_and_sensor_inactive_thenReturn_NoAlarm() {
        sensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        securityService.processImage(null);
        Mockito.verify(securityRepository, Mockito.times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //TEST 9
    @Test
    void changeSensorActivationStatusTest_alarm_disarmed_thenReturn_NoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        ArgumentCaptor<AlarmStatus> argumentCaptor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository).setAlarmStatus(argumentCaptor.capture());
        assertEquals(AlarmStatus.NO_ALARM, argumentCaptor.getValue());
    }

    //TEST 10
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names ={"ARMED_HOME", "ARMED_AWAY"})
    void changeSensorActivationStatusTest_system_armed_thenReturn_inactivated_SensorStatus(ArmingStatus armingStatus) {
        sensor.setActive(true);
        secondSensor.setActive(true);
        Set<Sensor> sensors = new HashSet<>(Arrays.asList(sensor, secondSensor));
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(armingStatus);
        assertFalse(sensor.getActive());
        assertFalse(secondSensor.getActive());
    }

    //TEST 11
    @Test
    void imageTest_system_armed_while_cat_detected_thenReturn_Alarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        ArgumentCaptor<AlarmStatus> argumentCaptor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository).setAlarmStatus(argumentCaptor.capture());
        assertEquals(AlarmStatus.ALARM, argumentCaptor.getValue());
    }

}