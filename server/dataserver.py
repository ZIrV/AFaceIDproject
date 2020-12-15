# -*- coding: UTF-8 -*-
# filename: dataserver date: 2018/11/21 15:23  
# author: FD 
import socketserver
import time


class FileServer(socketserver.BaseRequestHandler):
    def handle(self):
        filename = time.strftime('%Y-%m-%d-%H-%M-%S', time.localtime(time.time()))

        print('connected from:', self.client_address)
        with self.request:
            print(self.request.recv(1024))
            with open(filename+".zip", 'wb') as file:
                while True:
                    rdata = self.request.recv(1024)
                    if (len(rdata) == 0):
                        break
                    file.write(rdata)

s1 = socketserver.ThreadingTCPServer(('192.168.0.117', 30000), FileServer)
print('hello world')
s1.serve_forever()
