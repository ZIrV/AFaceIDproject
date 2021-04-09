import numpy as np
import math
import matplotlib.pyplot as plt
from scipy.signal import butter, lfilter, find_peaks_cwt, spectrogram, convolve2d
from statsmodels.tsa.seasonal import seasonal_decompose
# from scipy.signal import find_peaks
from scipy.fftpack import fft
from scipy.fftpack import ifft
from scipy.signal import stft
import os
import re
import time
import sys

fs = 48000


def main():
    getData()
    return

def medfilter (data):
    pdata=data.copy()
    for j in range(3, len(data)-2):
        pdata[j] = 0.15 * data[j - 2] + 0.2 * data[j - 1] + 0.3 * data[j] + 0.2 * data[j + 1] + 0.15 * data[j + 2]
    return pdata

def getData():
    filepath = './data/'
    files=os.listdir(filepath)
    for file in files:
        pattern = re.compile(r'\d+')
        res = re.findall(pattern,file)
        if True:
        # if len(res) == 3 and int(res[0]) == 0 and int(res[1]) >= 0 and int(res[1]) < 1:
            filename = filepath+file
            starttime = time.time()
            rawdata = np.memmap(filename, dtype=np.float32, mode='r')
            # plt.figure()
            # plt.plot(rawdata)
            # np.set_printoptions(threshold=sys.maxsize)
            # print(rawdata,file=open('./result/test.txt','a'))
            plt.figure()
            for channelID in [3]:
                fc = 17350+700*channelID
                data = butter_bandpass_filter(rawdata, fc-150, fc+150, 48000)
                data = data[48000:]
                # freq, t, zxx = spectrogram(data, fs=fs, nperseg=2048, noverlap=1024, nfft=48000)
                # freq = freq[fc-50:fc+51]
                # zxx = zxx[fc-50:fc+51, :]
                # zxx = np.abs(np.diff(zxx))*100000000
                # conv_factor = np.ones((3, 3))
                # zxx2 = convolve2d(zxx, conv_factor, mode="same")

                letter_size = 35
                font = {'weight': 'normal', 'size': letter_size}
                # plt.figure()
                # plt.pcolormesh(t, freq, zxx2)
                # plt.ylim((fc-50, fc+50))
                # plt.xlabel('Time (s)',font)
                # plt.ylabel('Frequency(kHz)',font)
                # plt.tick_params(labelsize=letter_size)

                f = fc
                I1 = getI(data, f)
                I = move_average_overlap(I1)
                Q1 = getQ(data, f)
                Q = move_average_overlap(Q1)

                decompositionQ = seasonal_decompose(Q, freq=10, two_sided=False)
                trendQ = decompositionQ.trend
                # plt.figure()
                trendQ = np.hstack((np.array([trendQ[10]] * 10), trendQ[10:]))
                decompositionI = seasonal_decompose(I, freq=10, two_sided=False)
                trendI = decompositionI.trend
                trendI = np.hstack((np.array([trendI[10]] * 10), trendI[10:]))
                # plt.plot(trendI)
                signaldata=[]
                for i in range (0, trendI.shape[0]):
                    signalsample=complex(trendQ[i],trendI[i])
                    signaldata.append(signalsample)
                signal_ph = np.angle(signaldata)
                signal_ph = np.unwrap(signal_ph)
                # plt.plot(signal_ph, '.')
                signal_am = np.abs(signaldata)
                signal_ph = medfilter(signal_ph)
                signal_am = medfilter(signal_am)
                signal_ph = medfilter(signal_ph)
                signal_am = medfilter(signal_am)
                # signal_ph = butter_lowpass_filter(signal_ph, 100, fs=480, order=5)
                # signal_am = butter_lowpass_filter(signal_am, 20, fs=480, order=5)
                signal_am = 10 * np.log10(signal_am ** 2)

                signal_diffph = [0]*1
                for i in range(1, len(signal_ph)):
                    signal_diffph.append((signal_ph[i] - signal_ph[i-1]))
                signal_diffam = [0]*1
                for i in range(1, len(signal_am)):
                    signal_diffam.append((signal_am[i] - signal_am[i-1]))
                signal_diffph = np.array(signal_diffph)
                signal_diffam = np.array(signal_diffam)
                nowtime = time.time()
                dur1 = nowtime - starttime
                print(dur1)
                t = [i/480 for i in range(0, len(signal_ph))]
                # plt.figure()
                channelID = '%d' % channelID
                chID = 'channel=' + channelID
                plt.plot(t, signal_diffam, label=chID)
                # plt.xlabel('Time(s)', font)
                # plt.ylabel('Phase(rad)', font)
                # plt.tick_params(labelsize=letter_size)
                # plt.legend(loc=3, fontsize=25)
    plt.show()

def chord_extract(trendI, trendQ):
    datachord = []
    for i in range(10, trendI.shape[0]):
        sample = ((trendI[i]-trendI[i-10])**2+(trendQ[i]-trendQ[i-10])**2)**0.5
        datachord.append(sample)
    return datachord




def getPhase1(I, Q):
    derivativeQ = getDerivative(Q)
    derivativeI = getDerivative(I)
    derivativeQ = np.asarray(derivativeQ)
    derivativeQ[np.where(derivativeQ==0)]=0.000001
    arcValue = np.arctan(-np.asarray(derivativeI) / (derivativeQ))
    newData = unwrap(arcValue)
    plt.plot(newData)
    plt.show()


def unwrap(data):
    resultData = []
    diffs = np.roll(data, -1) - data
    diffs = diffs[:len(data) - 1]
    first_value = data[0]
    resultData.append(first_value)
    previous_value = first_value
    current_value=None
    for diff in diffs:
        if diff > np.pi / 2:
            current_value = previous_value + diff - np.pi
            resultData.append(current_value)
        elif diff < -np.pi / 2:
            current_value = previous_value + diff + np.pi
            resultData.append(current_value)
        else:
            current_value=previous_value+diff
            resultData.append(current_value)
        previous_value = current_value
    return np.asarray(resultData)

def getDerivative(data):
    derivativeQ = []
    for i in range(len(data) - 1):
        derivativeQ.append((data[i + 1] - data[i]))
    return derivativeQ


def removeDC(data):
    return data - np.mean(data)


def distanceLine(phase, freq):
    distances = np.zeros(len(phase) - 1)
    for i in np.arange(1, len(phase)):
        phaseDiff = phase[0] - phase[i]
        distanceDiff = 343 / (2 * np.pi * freq) * phaseDiff
        distances[i - 1] = distanceDiff
    distances = distances / 2
    return distances


def getPhase(Q, I):
    if I == 0 and Q > 0:
        return np.pi / 2
    elif I == 0 and Q < 0:
        return 3 / 2 * np.pi
    elif Q == 0 and I > 0:
        return 0
    elif Q == 0 and I < 0:
        return np.pi
    tanValue = Q / I
    tanPhase = np.arctan(tanValue)
    resultPhase = 0
    if I > 0 and Q > 0:
        resultPhase = tanPhase
    elif I < 0 and Q > 0:
        resultPhase = np.pi + tanPhase
    elif I < 0 and Q < 0:
        resultPhase = np.pi + tanPhase
    elif I > 0 and Q < 0:
        resultPhase = 2 * np.pi + tanPhase
    return resultPhase


def move_average(data):
    win_size = 200
    new_len = len(data) // win_size
    data = data[0:new_len * win_size]
    data = data.reshape((new_len, win_size))
    result = np.zeros(new_len)
    for i in range(new_len):
        result[i] = np.mean(data[i, :])
    return result


def move_average_overlap(data):
    win_size = 200
    new_len = len(data) // win_size
    data = data[0:new_len * win_size]
    new_len = new_len*2
    result = np.zeros(new_len)
    for index in range(0, new_len):
        start =  (index/2)*win_size
        end = (index/2+1)*win_size
        result[index] = np.mean(data[int(start):int(end)])
    return result

def getI(data, f):
    times = np.arange(0, len(data)) * 1 / fs
    mulCos = np.cos(2 * np.pi * f * times) * data
    return mulCos



def getQ(data, f):
    times = np.arange(0, len(data)) * 1 / fs
    mulSin = -np.sin(2 * np.pi * f * times) * data
    return mulSin


def butter_lowpass(cutoff, fs, order=5):
    nyq = 0.5 * fs
    normal_cutoff = cutoff / nyq
    b, a = butter(order, normal_cutoff, btype='low', analog=False)
    return b, a


def butter_lowpass_filter(data, cutoff, fs, order=5):
    b, a = butter_lowpass(cutoff, fs, order=order)
    y = lfilter(b, a, data)
    return y


def butter_bandpass(lowcut, highcut, fs, order=5):
    nyq = 0.5 * fs
    low = lowcut / nyq
    high = highcut / nyq
    b, a = butter(order, [low, high], btype='band')
    return b, a


def butter_bandpass_filter(data, lowcut, highcut, fs, order=5):
    b, a = butter_bandpass(lowcut, highcut, fs, order)
    y = lfilter(b, a, data)
    return y


if __name__ == '__main__':
    main()
