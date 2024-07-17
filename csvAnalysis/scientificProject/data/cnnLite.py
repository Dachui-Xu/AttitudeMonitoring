import tensorflow as tf

# 载入原始的Keras模型
model = tf.keras.models.load_model('cnn_model.h5')

# 转换模型为TensorFlow Lite格式
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

# 保存转换后的模型
with open('model.tflite', 'wb') as f:
    f.write(tflite_model)
