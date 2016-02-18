#!/usr/bin/env python3

import flask
import gevent.wsgi

app = flask.Flask(__name__)


@app.route('/health')
def health():
    return 'OK'


@app.route('/ws/customerService', methods=['GET'])
def wsdl():
    with open('customer-service.wsdl') as fd:
        contents = fd.read()
    host = flask.request.headers['Host']
    is_local = host.split(':')[0] == 'localhost'
    location = '{}://{}/ws/customerService'.format('http' if is_local else 'https', host)
    return contents.replace('{{location}}', location)


@app.route('/ws/customerService', methods=['POST'])
def authenticate():
    '''<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body><ns2:authenticate xmlns:ns2="http://service.webservice.user.zalando.de/"><email>jdoe@example.org</email><password>foo</pas
    sword></ns2:authenticate></soap:Body></soap:Envelope>'''

    if b'goodpass' in flask.request.data:
        number = hash(flask.request.data)
        result = 'SUCCESS'
    else:
        number = '123'
        result = 'FAIL'

    response = '''<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
    <soap:Body><ns2:authenticateResponse xmlns:ns2="http://service.webservice.customer.zalando.de/" xmlns:ns3="http://ws.zalando.de/bm/shop">
    <return>
    <appDomainId>1</appDomainId>
    <customerNumber>{number}</customerNumber>
    <firstname>John</firstname>
    <gender>MALE</gender>
    <lastname>Doe</lastname>
    <loginResult>{result}</loginResult></return></ns2:authenticateResponse></soap:Body></soap:Envelope>'''.format(number=number, result=result)
    resp = flask.Response(response)
    resp.headers['Content-Type'] = 'text/xml'
    return resp

if __name__ == "__main__":
    http_server = gevent.wsgi.WSGIServer(('', 8080), app)
    http_server.serve_forever()
