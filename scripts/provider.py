#!/usr/bin/env python3
'''
CLI client for the Plan B Open ID Provider REST API

TODO: migrate to use https://github.com/zalando/openapi-cli-client
'''

import base64
import bcrypt
import click
import json
import requests

from clickclick import Action, AliasedGroup


@click.group(cls=AliasedGroup)
def cli():
    pass


@cli.group(cls=AliasedGroup)
def clients():
    pass


@cli.group(cls=AliasedGroup)
def users():
    pass


def get_b64hash(secret):
    bcrypt_hash = bcrypt.hashpw(secret.encode('utf-8'), bcrypt.gensalt())
    return base64.b64encode(bcrypt_hash).decode('utf-8')


@clients.command('create')
@click.argument('realm_client_id')
@click.option('--url')
@click.option('--password', prompt=True, hide_input=True)
@click.option('--non-confidential', is_flag=True, default=False)
def create_client(realm_client_id, url, password, non_confidential):
    '''
    Create/update client
    '''
    if not url:
        raise click.UsageError('Missing URL (--url)')
    data = {'secret_hash': get_b64hash(password),
            'is_confidential': not non_confidential}
    with Action('Creating client {}..'.format(realm_client_id)):
        r = requests.put(url + '/clients/{}'.format(realm_client_id.lstrip('/')), data=json.dumps(data),
                         headers={'Content-Type': 'application/json'})
        print(r.text)
        r.raise_for_status()


@users.command('create')
@click.argument('realm_username')
@click.option('--url')
@click.option('--password', prompt=True, hide_input=True)
def create_user(realm_username, url, password):
    '''
    Create/update user
    '''
    if not url:
        raise click.UsageError('Missing URL (--url)')
    data = {'password_hashes': [get_b64hash(password)]}
    with Action('Creating user {}..'.format(realm_username)):
        r = requests.put(url + '/users/{}'.format(realm_username.lstrip('/')), data=json.dumps(data),
                         headers={'Content-Type': 'application/json'})
        print(r.text)
        r.raise_for_status()


if __name__ == '__main__':
    cli()
