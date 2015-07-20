#! /usr/bin/env python

def process(transaction):
    dataset = transaction.createNewDataSet()
    transaction.moveFile(transaction.getIncoming().getPath(), dataset)