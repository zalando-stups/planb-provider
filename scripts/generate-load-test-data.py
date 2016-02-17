#!/usr/bin/env python3

import bcrypt
import base64
import time
from subprocess import check_call


check_call('openssl genrsa -out test-rs256-2048.pem 2048', shell=True)
with open('test-rs256-2048.pem') as fd:
    rs256 = fd.read()
check_call('openssl ecparam -genkey -out test-es512-secp521r1.pem -name secp521r1', shell=True)
with open('test-es512-secp521r1.pem') as fd:
    es512 = fd.read()

now = str(int(time.time()))
print('''
INSERT INTO provider.keypair
        (kid, realms, private_key_pem, algorithm, valid_from)
              VALUES
                      ('testkey-services', {'/services'}, ''' + "'" + rs256 + "', 'RS256', " + now + ");")


print('''
INSERT INTO provider.keypair
        (kid, realms, private_key_pem, algorithm, valid_from)
              VALUES
                      ('testkey-customers', {'/customers'}, ''' + "'" + es512 + "', 'ES512', " + now + ");")

for i in range(2048):
    uid = 'test{}'.format(i)
    pw = base64.b64encode(bcrypt.hashpw(uid.encode('utf-8'), bcrypt.gensalt())).decode('utf-8')
    print("INSERT INTO provider.client (client_id, realm, client_secret_hash, is_confidential, scopes) VALUES ('" + uid + "', '/services', '" + pw + "', true, {'uid'});")
    print("INSERT INTO provider.client (client_id, realm, client_secret_hash, is_confidential, scopes) VALUES ('" + uid + "', '/customers', '" + pw + "', true, {'uid'});")
    print("INSERT INTO provider.user (username, realm, password_hashes, scopes) VALUES ('" + uid + "', '/services', {'" + pw + "'}, {'uid': 'true'});")
