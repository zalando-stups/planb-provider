-- default users
-- hash is print(bcrypt.hashpw(b'test', bcrypt.gensalt(4)))
INSERT INTO provider.client (client_id, realm, client_secret_hash, scopes, is_confidential, created_by, last_modified_by)
    VALUES ('testclient', '/services', '$2b$04$iXpLcLet3KbvJ2MOEnd49u6sMTBAfRlLc43kehrCI.3OWbkIGcS7K',
            {'uid', 'ascope', 'clientonly'}, true, '/services/user1', '/services/user1');

INSERT INTO provider.client (client_id, realm, client_secret_hash, scopes, is_confidential, created_by, last_modified_by)
    VALUES ('testpublicclient', '/services', '$2b$04$iXpLcLet3KbvJ2MOEnd49u6sMTBAfRlLc43kehrCI.3OWbkIGcS7K',
            {'uid', 'ascope', 'clientonly'}, false, '/services/user1', '/services/user1');

INSERT INTO provider.client (client_id, realm, client_secret_hash, scopes, is_confidential, created_by, last_modified_by)
    VALUES ('testclient', '/customers', '$2b$04$iXpLcLet3KbvJ2MOEnd49u6sMTBAfRlLc43kehrCI.3OWbkIGcS7K',
            {'uid', 'ascope', 'clientonly'}, true, '/services/user1', '/services/user1');

INSERT INTO provider.client (client_id, realm, client_secret_hash, scopes, is_confidential, redirect_uris, created_by, last_modified_by)
    VALUES ('testredirectclient', '/services', '$2b$04$iXpLcLet3KbvJ2MOEnd49u6sMTBAfRlLc43kehrCI.3OWbkIGcS7K',
            {'uid', 'ascope', 'clientonly'}, false, {'https://myapp.example.org/callback'}, '/services/user1', '/services/user1');

INSERT INTO provider.user (username, realm, password_hashes, scopes, created_by, last_modified_by)
    VALUES ('testuser', '/services', { {password_hash: '$2b$04$iXpLcLet3KbvJ2MOEnd49u6sMTBAfRlLc43kehrCI.3OWbkIGcS7K', created: 123, created_by: 'testdata.cql'} },
            {'uid': 'uidval', 'ascope': 'ascopeval', 'useronly': 'useronlyval'}, '/services/user1', '/services/user1');

-- default keys

INSERT INTO provider.keypair (kid, realms, private_key_pem, algorithm, valid_from)
    VALUES ('test-rs256', {'/services', '/customers'}, '-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA3dEMoTXmK7/+WJJpnyjR6hzcG4fKhLfQ0i5VOyT7wny2yw0P
oP+lxaYQ/M+khkECcPoCElCfu2n2iRzrLkM2VoCNybScqFoAiClgEXUqC8x8P7VR
1rKkR3/7egDbS5H+lVtBtiYczXHqapQvA8S7tp/SozDfod6mqHXhMilXvPGU+81K
hkpSrXte7kwXQ/LyGGSSaeOKwTnhDCkKcTpBT0zVV9IRw5WLmrqfiWAsc4Dwxe/+
PQBc44oSIy80v55Oc5Ym3TY1qWHPKxKKwYO0xby8z7yfJNLBUUu66nsNJQD8dglU
92+4u7LnTwzIN4gaUdGSEiL4aiOwBCLtwUmNfwIDAQABAoIBAQCizX1skSfHspnW
nleBZq9zGg1+81JzyIouyazqxQE3RNvQ+lwfOaONBo7vTuOdWfeVvhlpId2q62H0
TtJaWSB3qtDmW3ajpbNAPMXy6kCa+lGzXLydTO6AP/HuINTTiWsGaj03mI+JRnPc
F7jOFkYECxfJiOSKZOWDPUm0zF1GIdcne32UJxFLawh8/GHE+NxuHcBK/JJrBGEt
pzqHyVFHiUasY1dxZGpoggmUGpPpZ8WrK8S+CU/yYyFH1WdPeMxPajFvFyvSLxmF
VTQyqAKUvbA18vrAIipKOd4aHq9apXH6AcghVUbQzoVUgHBMBqJwyNeTvmCfyvfn
fC8Ii0lJAoGBAP9sfleXs2UH89gv+HgKgJ9vYELs1y3h6pKOUbqK/IQ1Yl33cGEK
1H08ZxQBOIBTigJ+VZ0oyQXFTFXT3tECwmD9iIKB75evBGo/Y8wCmFBSx/e2VyxE
RHO6958ZvqXe28zQ9dnjctgcEkiUDVeyP/htOy9QPo+EIWYQ0WQkCfvVAoGBAN5R
JdMR/Sy+RS+5+U0jpM0bUGTAdYwbwnn/9bNVLo9B1DVBjkOb1D5U+nJc28trSll6
evaOtygiy+b3iExAdtnLcxfmCWGPKVcJx946GWVJM9p66wZ4UoMP29DmrMfKBgQx
SKB7PuXRR/lNa+6sjd2n1XIhQoIqxIxju29EDDIDAoGAI3ZhuDGUs6M+BLbsTWZP
41LoT4JogbNLCRv/VuHzGzv6M0emb6K8S8L2IL3mpVJz59K4ekBuYIG7DnODDQvQ
Bv1MVapKpImlGEdCyNFXaleD8e17/uZfhp3fwVQmtwrRA4fDioPcrwp+s1ry20kh
PpGT1QbUGMLkjDIrkDa8uB0CgYBiJ4PCr/OOuKcFTl/SfgU6BXA2O37qkCsKAEdz
mQ1IdLEDnmD4WqmXp583tOXZ5xHZdakqiJI3Jz3NSMalm+SdfiTfjVHg//spkYjs
BczGmk5JjPGNVrxfXzYXAnGQeBK18Sh2qlN31jGn0VXw98Be98XWcPbTT5yduz66
/llADwKBgQDy2g9e+WlvgUHQK76gPVX0LYeyl04HjL6f2cFRV2KB27H4rbOzVpV3
PfAYw8kJfub0KSTHDfBsP+4kCCQCZ9Cq6tWdk3CkJyCVJ1tOQ1eCt8JxW+mAkIa1
ZJleYgmy4L+qZ9SWZixHBxVZba+XLXQ4sdba7FwQ0CumHTbzuElKfg==
-----END RSA PRIVATE KEY-----
', 'RS256', 1000);
INSERT INTO provider.keypair (kid, realms, private_key_pem, algorithm, valid_from)
    VALUES ('test-rs384', {'/services', '/customers'}, '-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA3dEMoTXmK7/+WJJpnyjR6hzcG4fKhLfQ0i5VOyT7wny2yw0P
oP+lxaYQ/M+khkECcPoCElCfu2n2iRzrLkM2VoCNybScqFoAiClgEXUqC8x8P7VR
1rKkR3/7egDbS5H+lVtBtiYczXHqapQvA8S7tp/SozDfod6mqHXhMilXvPGU+81K
hkpSrXte7kwXQ/LyGGSSaeOKwTnhDCkKcTpBT0zVV9IRw5WLmrqfiWAsc4Dwxe/+
PQBc44oSIy80v55Oc5Ym3TY1qWHPKxKKwYO0xby8z7yfJNLBUUu66nsNJQD8dglU
92+4u7LnTwzIN4gaUdGSEiL4aiOwBCLtwUmNfwIDAQABAoIBAQCizX1skSfHspnW
nleBZq9zGg1+81JzyIouyazqxQE3RNvQ+lwfOaONBo7vTuOdWfeVvhlpId2q62H0
TtJaWSB3qtDmW3ajpbNAPMXy6kCa+lGzXLydTO6AP/HuINTTiWsGaj03mI+JRnPc
F7jOFkYECxfJiOSKZOWDPUm0zF1GIdcne32UJxFLawh8/GHE+NxuHcBK/JJrBGEt
pzqHyVFHiUasY1dxZGpoggmUGpPpZ8WrK8S+CU/yYyFH1WdPeMxPajFvFyvSLxmF
VTQyqAKUvbA18vrAIipKOd4aHq9apXH6AcghVUbQzoVUgHBMBqJwyNeTvmCfyvfn
fC8Ii0lJAoGBAP9sfleXs2UH89gv+HgKgJ9vYELs1y3h6pKOUbqK/IQ1Yl33cGEK
1H08ZxQBOIBTigJ+VZ0oyQXFTFXT3tECwmD9iIKB75evBGo/Y8wCmFBSx/e2VyxE
RHO6958ZvqXe28zQ9dnjctgcEkiUDVeyP/htOy9QPo+EIWYQ0WQkCfvVAoGBAN5R
JdMR/Sy+RS+5+U0jpM0bUGTAdYwbwnn/9bNVLo9B1DVBjkOb1D5U+nJc28trSll6
evaOtygiy+b3iExAdtnLcxfmCWGPKVcJx946GWVJM9p66wZ4UoMP29DmrMfKBgQx
SKB7PuXRR/lNa+6sjd2n1XIhQoIqxIxju29EDDIDAoGAI3ZhuDGUs6M+BLbsTWZP
41LoT4JogbNLCRv/VuHzGzv6M0emb6K8S8L2IL3mpVJz59K4ekBuYIG7DnODDQvQ
Bv1MVapKpImlGEdCyNFXaleD8e17/uZfhp3fwVQmtwrRA4fDioPcrwp+s1ry20kh
PpGT1QbUGMLkjDIrkDa8uB0CgYBiJ4PCr/OOuKcFTl/SfgU6BXA2O37qkCsKAEdz
mQ1IdLEDnmD4WqmXp583tOXZ5xHZdakqiJI3Jz3NSMalm+SdfiTfjVHg//spkYjs
BczGmk5JjPGNVrxfXzYXAnGQeBK18Sh2qlN31jGn0VXw98Be98XWcPbTT5yduz66
/llADwKBgQDy2g9e+WlvgUHQK76gPVX0LYeyl04HjL6f2cFRV2KB27H4rbOzVpV3
PfAYw8kJfub0KSTHDfBsP+4kCCQCZ9Cq6tWdk3CkJyCVJ1tOQ1eCt8JxW+mAkIa1
ZJleYgmy4L+qZ9SWZixHBxVZba+XLXQ4sdba7FwQ0CumHTbzuElKfg==
-----END RSA PRIVATE KEY-----
', 'RS384', 2000);
INSERT INTO provider.keypair (kid, realms, private_key_pem, algorithm, valid_from)
    VALUES ('test-rs512', {'/services', '/customers'}, '-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA3dEMoTXmK7/+WJJpnyjR6hzcG4fKhLfQ0i5VOyT7wny2yw0P
oP+lxaYQ/M+khkECcPoCElCfu2n2iRzrLkM2VoCNybScqFoAiClgEXUqC8x8P7VR
1rKkR3/7egDbS5H+lVtBtiYczXHqapQvA8S7tp/SozDfod6mqHXhMilXvPGU+81K
hkpSrXte7kwXQ/LyGGSSaeOKwTnhDCkKcTpBT0zVV9IRw5WLmrqfiWAsc4Dwxe/+
PQBc44oSIy80v55Oc5Ym3TY1qWHPKxKKwYO0xby8z7yfJNLBUUu66nsNJQD8dglU
92+4u7LnTwzIN4gaUdGSEiL4aiOwBCLtwUmNfwIDAQABAoIBAQCizX1skSfHspnW
nleBZq9zGg1+81JzyIouyazqxQE3RNvQ+lwfOaONBo7vTuOdWfeVvhlpId2q62H0
TtJaWSB3qtDmW3ajpbNAPMXy6kCa+lGzXLydTO6AP/HuINTTiWsGaj03mI+JRnPc
F7jOFkYECxfJiOSKZOWDPUm0zF1GIdcne32UJxFLawh8/GHE+NxuHcBK/JJrBGEt
pzqHyVFHiUasY1dxZGpoggmUGpPpZ8WrK8S+CU/yYyFH1WdPeMxPajFvFyvSLxmF
VTQyqAKUvbA18vrAIipKOd4aHq9apXH6AcghVUbQzoVUgHBMBqJwyNeTvmCfyvfn
fC8Ii0lJAoGBAP9sfleXs2UH89gv+HgKgJ9vYELs1y3h6pKOUbqK/IQ1Yl33cGEK
1H08ZxQBOIBTigJ+VZ0oyQXFTFXT3tECwmD9iIKB75evBGo/Y8wCmFBSx/e2VyxE
RHO6958ZvqXe28zQ9dnjctgcEkiUDVeyP/htOy9QPo+EIWYQ0WQkCfvVAoGBAN5R
JdMR/Sy+RS+5+U0jpM0bUGTAdYwbwnn/9bNVLo9B1DVBjkOb1D5U+nJc28trSll6
evaOtygiy+b3iExAdtnLcxfmCWGPKVcJx946GWVJM9p66wZ4UoMP29DmrMfKBgQx
SKB7PuXRR/lNa+6sjd2n1XIhQoIqxIxju29EDDIDAoGAI3ZhuDGUs6M+BLbsTWZP
41LoT4JogbNLCRv/VuHzGzv6M0emb6K8S8L2IL3mpVJz59K4ekBuYIG7DnODDQvQ
Bv1MVapKpImlGEdCyNFXaleD8e17/uZfhp3fwVQmtwrRA4fDioPcrwp+s1ry20kh
PpGT1QbUGMLkjDIrkDa8uB0CgYBiJ4PCr/OOuKcFTl/SfgU6BXA2O37qkCsKAEdz
mQ1IdLEDnmD4WqmXp583tOXZ5xHZdakqiJI3Jz3NSMalm+SdfiTfjVHg//spkYjs
BczGmk5JjPGNVrxfXzYXAnGQeBK18Sh2qlN31jGn0VXw98Be98XWcPbTT5yduz66
/llADwKBgQDy2g9e+WlvgUHQK76gPVX0LYeyl04HjL6f2cFRV2KB27H4rbOzVpV3
PfAYw8kJfub0KSTHDfBsP+4kCCQCZ9Cq6tWdk3CkJyCVJ1tOQ1eCt8JxW+mAkIa1
ZJleYgmy4L+qZ9SWZixHBxVZba+XLXQ4sdba7FwQ0CumHTbzuElKfg==
-----END RSA PRIVATE KEY-----
', 'RS512', 3000);

INSERT INTO provider.keypair (kid, realms, private_key_pem, algorithm, valid_from)
    VALUES ('test-ps256', {'/services', '/customers'}, '-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA3dEMoTXmK7/+WJJpnyjR6hzcG4fKhLfQ0i5VOyT7wny2yw0P
oP+lxaYQ/M+khkECcPoCElCfu2n2iRzrLkM2VoCNybScqFoAiClgEXUqC8x8P7VR
1rKkR3/7egDbS5H+lVtBtiYczXHqapQvA8S7tp/SozDfod6mqHXhMilXvPGU+81K
hkpSrXte7kwXQ/LyGGSSaeOKwTnhDCkKcTpBT0zVV9IRw5WLmrqfiWAsc4Dwxe/+
PQBc44oSIy80v55Oc5Ym3TY1qWHPKxKKwYO0xby8z7yfJNLBUUu66nsNJQD8dglU
92+4u7LnTwzIN4gaUdGSEiL4aiOwBCLtwUmNfwIDAQABAoIBAQCizX1skSfHspnW
nleBZq9zGg1+81JzyIouyazqxQE3RNvQ+lwfOaONBo7vTuOdWfeVvhlpId2q62H0
TtJaWSB3qtDmW3ajpbNAPMXy6kCa+lGzXLydTO6AP/HuINTTiWsGaj03mI+JRnPc
F7jOFkYECxfJiOSKZOWDPUm0zF1GIdcne32UJxFLawh8/GHE+NxuHcBK/JJrBGEt
pzqHyVFHiUasY1dxZGpoggmUGpPpZ8WrK8S+CU/yYyFH1WdPeMxPajFvFyvSLxmF
VTQyqAKUvbA18vrAIipKOd4aHq9apXH6AcghVUbQzoVUgHBMBqJwyNeTvmCfyvfn
fC8Ii0lJAoGBAP9sfleXs2UH89gv+HgKgJ9vYELs1y3h6pKOUbqK/IQ1Yl33cGEK
1H08ZxQBOIBTigJ+VZ0oyQXFTFXT3tECwmD9iIKB75evBGo/Y8wCmFBSx/e2VyxE
RHO6958ZvqXe28zQ9dnjctgcEkiUDVeyP/htOy9QPo+EIWYQ0WQkCfvVAoGBAN5R
JdMR/Sy+RS+5+U0jpM0bUGTAdYwbwnn/9bNVLo9B1DVBjkOb1D5U+nJc28trSll6
evaOtygiy+b3iExAdtnLcxfmCWGPKVcJx946GWVJM9p66wZ4UoMP29DmrMfKBgQx
SKB7PuXRR/lNa+6sjd2n1XIhQoIqxIxju29EDDIDAoGAI3ZhuDGUs6M+BLbsTWZP
41LoT4JogbNLCRv/VuHzGzv6M0emb6K8S8L2IL3mpVJz59K4ekBuYIG7DnODDQvQ
Bv1MVapKpImlGEdCyNFXaleD8e17/uZfhp3fwVQmtwrRA4fDioPcrwp+s1ry20kh
PpGT1QbUGMLkjDIrkDa8uB0CgYBiJ4PCr/OOuKcFTl/SfgU6BXA2O37qkCsKAEdz
mQ1IdLEDnmD4WqmXp583tOXZ5xHZdakqiJI3Jz3NSMalm+SdfiTfjVHg//spkYjs
BczGmk5JjPGNVrxfXzYXAnGQeBK18Sh2qlN31jGn0VXw98Be98XWcPbTT5yduz66
/llADwKBgQDy2g9e+WlvgUHQK76gPVX0LYeyl04HjL6f2cFRV2KB27H4rbOzVpV3
PfAYw8kJfub0KSTHDfBsP+4kCCQCZ9Cq6tWdk3CkJyCVJ1tOQ1eCt8JxW+mAkIa1
ZJleYgmy4L+qZ9SWZixHBxVZba+XLXQ4sdba7FwQ0CumHTbzuElKfg==
-----END RSA PRIVATE KEY-----
', 'PS256', 4000);
INSERT INTO provider.keypair (kid, realms, private_key_pem, algorithm, valid_from)
    VALUES ('test-ps384', {'/services', '/customers'}, '-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA3dEMoTXmK7/+WJJpnyjR6hzcG4fKhLfQ0i5VOyT7wny2yw0P
oP+lxaYQ/M+khkECcPoCElCfu2n2iRzrLkM2VoCNybScqFoAiClgEXUqC8x8P7VR
1rKkR3/7egDbS5H+lVtBtiYczXHqapQvA8S7tp/SozDfod6mqHXhMilXvPGU+81K
hkpSrXte7kwXQ/LyGGSSaeOKwTnhDCkKcTpBT0zVV9IRw5WLmrqfiWAsc4Dwxe/+
PQBc44oSIy80v55Oc5Ym3TY1qWHPKxKKwYO0xby8z7yfJNLBUUu66nsNJQD8dglU
92+4u7LnTwzIN4gaUdGSEiL4aiOwBCLtwUmNfwIDAQABAoIBAQCizX1skSfHspnW
nleBZq9zGg1+81JzyIouyazqxQE3RNvQ+lwfOaONBo7vTuOdWfeVvhlpId2q62H0
TtJaWSB3qtDmW3ajpbNAPMXy6kCa+lGzXLydTO6AP/HuINTTiWsGaj03mI+JRnPc
F7jOFkYECxfJiOSKZOWDPUm0zF1GIdcne32UJxFLawh8/GHE+NxuHcBK/JJrBGEt
pzqHyVFHiUasY1dxZGpoggmUGpPpZ8WrK8S+CU/yYyFH1WdPeMxPajFvFyvSLxmF
VTQyqAKUvbA18vrAIipKOd4aHq9apXH6AcghVUbQzoVUgHBMBqJwyNeTvmCfyvfn
fC8Ii0lJAoGBAP9sfleXs2UH89gv+HgKgJ9vYELs1y3h6pKOUbqK/IQ1Yl33cGEK
1H08ZxQBOIBTigJ+VZ0oyQXFTFXT3tECwmD9iIKB75evBGo/Y8wCmFBSx/e2VyxE
RHO6958ZvqXe28zQ9dnjctgcEkiUDVeyP/htOy9QPo+EIWYQ0WQkCfvVAoGBAN5R
JdMR/Sy+RS+5+U0jpM0bUGTAdYwbwnn/9bNVLo9B1DVBjkOb1D5U+nJc28trSll6
evaOtygiy+b3iExAdtnLcxfmCWGPKVcJx946GWVJM9p66wZ4UoMP29DmrMfKBgQx
SKB7PuXRR/lNa+6sjd2n1XIhQoIqxIxju29EDDIDAoGAI3ZhuDGUs6M+BLbsTWZP
41LoT4JogbNLCRv/VuHzGzv6M0emb6K8S8L2IL3mpVJz59K4ekBuYIG7DnODDQvQ
Bv1MVapKpImlGEdCyNFXaleD8e17/uZfhp3fwVQmtwrRA4fDioPcrwp+s1ry20kh
PpGT1QbUGMLkjDIrkDa8uB0CgYBiJ4PCr/OOuKcFTl/SfgU6BXA2O37qkCsKAEdz
mQ1IdLEDnmD4WqmXp583tOXZ5xHZdakqiJI3Jz3NSMalm+SdfiTfjVHg//spkYjs
BczGmk5JjPGNVrxfXzYXAnGQeBK18Sh2qlN31jGn0VXw98Be98XWcPbTT5yduz66
/llADwKBgQDy2g9e+WlvgUHQK76gPVX0LYeyl04HjL6f2cFRV2KB27H4rbOzVpV3
PfAYw8kJfub0KSTHDfBsP+4kCCQCZ9Cq6tWdk3CkJyCVJ1tOQ1eCt8JxW+mAkIa1
ZJleYgmy4L+qZ9SWZixHBxVZba+XLXQ4sdba7FwQ0CumHTbzuElKfg==
-----END RSA PRIVATE KEY-----
', 'PS384', 5000);
INSERT INTO provider.keypair (kid, realms, private_key_pem, algorithm, valid_from)
    VALUES ('test-ps512', {'/services', '/customers'}, '-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA3dEMoTXmK7/+WJJpnyjR6hzcG4fKhLfQ0i5VOyT7wny2yw0P
oP+lxaYQ/M+khkECcPoCElCfu2n2iRzrLkM2VoCNybScqFoAiClgEXUqC8x8P7VR
1rKkR3/7egDbS5H+lVtBtiYczXHqapQvA8S7tp/SozDfod6mqHXhMilXvPGU+81K
hkpSrXte7kwXQ/LyGGSSaeOKwTnhDCkKcTpBT0zVV9IRw5WLmrqfiWAsc4Dwxe/+
PQBc44oSIy80v55Oc5Ym3TY1qWHPKxKKwYO0xby8z7yfJNLBUUu66nsNJQD8dglU
92+4u7LnTwzIN4gaUdGSEiL4aiOwBCLtwUmNfwIDAQABAoIBAQCizX1skSfHspnW
nleBZq9zGg1+81JzyIouyazqxQE3RNvQ+lwfOaONBo7vTuOdWfeVvhlpId2q62H0
TtJaWSB3qtDmW3ajpbNAPMXy6kCa+lGzXLydTO6AP/HuINTTiWsGaj03mI+JRnPc
F7jOFkYECxfJiOSKZOWDPUm0zF1GIdcne32UJxFLawh8/GHE+NxuHcBK/JJrBGEt
pzqHyVFHiUasY1dxZGpoggmUGpPpZ8WrK8S+CU/yYyFH1WdPeMxPajFvFyvSLxmF
VTQyqAKUvbA18vrAIipKOd4aHq9apXH6AcghVUbQzoVUgHBMBqJwyNeTvmCfyvfn
fC8Ii0lJAoGBAP9sfleXs2UH89gv+HgKgJ9vYELs1y3h6pKOUbqK/IQ1Yl33cGEK
1H08ZxQBOIBTigJ+VZ0oyQXFTFXT3tECwmD9iIKB75evBGo/Y8wCmFBSx/e2VyxE
RHO6958ZvqXe28zQ9dnjctgcEkiUDVeyP/htOy9QPo+EIWYQ0WQkCfvVAoGBAN5R
JdMR/Sy+RS+5+U0jpM0bUGTAdYwbwnn/9bNVLo9B1DVBjkOb1D5U+nJc28trSll6
evaOtygiy+b3iExAdtnLcxfmCWGPKVcJx946GWVJM9p66wZ4UoMP29DmrMfKBgQx
SKB7PuXRR/lNa+6sjd2n1XIhQoIqxIxju29EDDIDAoGAI3ZhuDGUs6M+BLbsTWZP
41LoT4JogbNLCRv/VuHzGzv6M0emb6K8S8L2IL3mpVJz59K4ekBuYIG7DnODDQvQ
Bv1MVapKpImlGEdCyNFXaleD8e17/uZfhp3fwVQmtwrRA4fDioPcrwp+s1ry20kh
PpGT1QbUGMLkjDIrkDa8uB0CgYBiJ4PCr/OOuKcFTl/SfgU6BXA2O37qkCsKAEdz
mQ1IdLEDnmD4WqmXp583tOXZ5xHZdakqiJI3Jz3NSMalm+SdfiTfjVHg//spkYjs
BczGmk5JjPGNVrxfXzYXAnGQeBK18Sh2qlN31jGn0VXw98Be98XWcPbTT5yduz66
/llADwKBgQDy2g9e+WlvgUHQK76gPVX0LYeyl04HjL6f2cFRV2KB27H4rbOzVpV3
PfAYw8kJfub0KSTHDfBsP+4kCCQCZ9Cq6tWdk3CkJyCVJ1tOQ1eCt8JxW+mAkIa1
ZJleYgmy4L+qZ9SWZixHBxVZba+XLXQ4sdba7FwQ0CumHTbzuElKfg==
-----END RSA PRIVATE KEY-----
', 'PS512', 6000);

INSERT INTO provider.keypair (kid, realms, private_key_pem, algorithm, valid_from)
    VALUES ('test-es256', {'/customers'}, '-----BEGIN EC PARAMETERS-----
BggqhkjOPQMBBw==
-----END EC PARAMETERS-----
-----BEGIN EC PRIVATE KEY-----
MHcCAQEEIFbjnfrpqI7Vjf3GnzosN+iS2oBPEJ+rmBqFcxIM/epNoAoGCCqGSM49
AwEHoUQDQgAEou0O0sqz0/TybUS+Z3O20bx0ow8lWm06cgu19+hFYvnyc+cbBfTT
FzSC+JUx7p/2D5NRdkIciJbGuqToQiRg1w==
-----END EC PRIVATE KEY-----
', 'ES256', 7000);
INSERT INTO provider.keypair (kid, realms, private_key_pem, algorithm, valid_from)
    VALUES ('test-es384', {'/services'}, '-----BEGIN EC PARAMETERS-----
BgUrgQQAIg==
-----END EC PARAMETERS-----
-----BEGIN EC PRIVATE KEY-----
MIGkAgEBBDA2vTaw2NkC8+f8yRGicalwWgxcSe76VUtdLRt+VAt5RLAY3LGtDMMK
JrSZ+i6MipGgBwYFK4EEACKhZANiAAQ+MrWb90zTTVP8uuw1lmaKWoWl6dJYgdcE
gYpxt3h/Y0Zm9ClgCgjkLupoRE7SRRg5/mZhg0huqMm/uyFwmQByIfvkvNCKdnbE
ydwZ0xPNbC2rbjHKQ6zI+uGCz/Ph2Zs=
-----END EC PRIVATE KEY-----
', 'ES384', 8000);  -- should be the active
INSERT INTO provider.keypair (kid, realms, private_key_pem, algorithm, valid_from)
    VALUES ('test-es512', {'/services', '/customers'}, '-----BEGIN EC PARAMETERS-----
BgUrgQQAIw==
-----END EC PARAMETERS-----
-----BEGIN EC PRIVATE KEY-----
MIHcAgEBBEIBak4s6MGgdS6/uK+EfM+nSIUA65HLHqi+w1+KV7vLJP5HvbSaSd7I
msdhpUa0cj56C7rhxncjwv6/H/WQgzXWXIKgBwYFK4EEACOhgYkDgYYABAFbrsYO
1lxTWWQPzNyMzjz2FtTdk6rgCeWZOWkai4LxrzyQVDRLPUWk+Su+rD4g7KEJvgt4
W/T7QTmNTNy+vzS+WAEyxB3rNSkq67laMQCqEMRjrHRA7J8T1IUMLm4KZkt2QbLg
EFB7Il0CE/88jfZSWSx35kP5sElMWL06rDV2apF1Rg==
-----END EC PRIVATE KEY-----
', 'ES512', 1855570704);
