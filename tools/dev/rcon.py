import socket, struct, sys, time
def pkt(s, rid, typ, body):
    b = body.encode('utf-8')
    s.sendall(struct.pack('<iii', len(b)+10, rid, typ) + b + b'\x00\x00')
def recv(s):
    hdr = b''
    while len(hdr) < 4: hdr += s.recv(4-len(hdr))
    ln = struct.unpack('<i', hdr)[0]; data=b''
    while len(data) < ln: data += s.recv(ln-len(data))
    rid, typ = struct.unpack('<ii', data[:8]); return rid, data[8:-2].decode('utf-8','replace')
s = socket.create_connection(('127.0.0.1', 25575), timeout=900)
pkt(s, 1, 3, 'test'); print('auth', recv(s)[0])
for cmd in sys.argv[1:]:
    t=time.time(); pkt(s, 2, 2, cmd); rid, body = recv(s)
    print(f'> {cmd}  ({time.time()-t:.1f}s)\n{body}')
