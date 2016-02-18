#!/usr/bin/env python3
print('username,password')
for i in range(2048):
    uid = 'test{}'.format(i)
    print('{},{}'.format(uid, uid))
