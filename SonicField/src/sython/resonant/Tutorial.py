import time
tStamp = str(int(time.time()))
def main():
    signal = sf.WhiteNoise(60000)
    sf.WriteFile32([signal], 'temp/tutorial/tutorial{}_{}.wav'.format(0, tStamp))
    for c in xrange(1, 11):
        signal = sf.RBJPeaking(signal, 1000, 0.1, 10.0)
        signal = sf.FixSize(signal)
        sf.WriteFile32([signal], 'temp/tutorial/tutorial{}_{}.wav'.format(c, tStamp))