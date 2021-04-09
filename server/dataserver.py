# -*- coding: UTF-8 -*-
# filename: dataserver date: 2018/11/21 15:23  
# author: FD 
import socketserver
import socket
import time


class FileServer(socketserver.BaseRequestHandler):
    def handle(self):
        filename = './data/test17'
        print('connected from:', self.client_address)
        with open(filename+".pcm", 'wb') as file:
            with self.request:
                while True:
                    rdata = self.request.recv(1024)
                    if (len(rdata) == 0):
                        print(1)
                        break
                    file.write(rdata)
                    #print(len(rdata))
                    

s1 = socketserver.ThreadingTCPServer(('192.168.1.2', 30000), FileServer)
print('hello world')

s1.serve_forever()
