import os
import json
from tqdm import tqdm


def convert(weight, height, box):

    x = (box[0] + box[2]) / 2.0 / weight
    y = (box[1] + box[3]) / 2.0 / height
    w = (box[2] - box[0]) / weight
    h = (box[3] - box[1]) / height

    return (x, y, w, h)


if __name__ == '__main__':
    json_path = './data/datasetXGN/annotations/'
    txt_path = './data/datasetXGN/yolo_annotations/'
    if not os.path.exists(txt_path):
        os.makedirs(txt_path)
    for json_file in tqdm(os.listdir(json_path)):
        data = json.load(open(os.path.join(json_path, json_file), 'r'))
        if not data or not data['bbs']:
            continue
        with open(os.path.join(txt_path, '%s.txt' % data["img"].split('.')[0]), 'w') as f:
            box = convert(data['weidth'], data['height'],
                          data['bbs'][0]['bbx'])
            f.write("%s %s %s %s %s\n" % ('0', box[0], box[1], box[2], box[3]))
