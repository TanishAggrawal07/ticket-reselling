const cloudinary = require('cloudinary').v2;
const { v4: uuidv4 } = require('uuid');

// Configure Cloudinary
const cloudName = process.env.CLOUDINARY_CLOUD_NAME;
const apiKey = process.env.CLOUDINARY_API_KEY;
const apiSecret = process.env.CLOUDINARY_API_SECRET;

// Debug logging (remove in production)
console.log('Cloudinary config:', {
    cloud_name: cloudName ? 'SET' : 'NOT SET',
    api_key: apiKey ? 'SET' : 'NOT SET',
    api_secret: apiSecret ? 'SET' : 'NOT SET'
});

if (!cloudName || !apiKey || !apiSecret) {
    console.error('ERROR: Cloudinary environment variables are not set!');
}

cloudinary.config({
    cloud_name: cloudName,
    api_key: apiKey,
    api_secret: apiSecret
});

const FOLDER = process.env.CLOUDINARY_FOLDER || 'retix_tickets';

/**
 * Upload a file buffer to Cloudinary
 * @param {Buffer} buffer - File buffer
 * @param {string} mimetype - File mime type
 * @param {string} resourceType - 'image' or 'raw' (for PDFs)
 * @returns {Promise<string>} Secure URL of uploaded file
 */
async function uploadImage(buffer, mimetype, resourceType) {
    resourceType = resourceType || 'image';
    return new Promise((resolve, reject) => {
        const uploadOptions = {
            folder: FOLDER,
            public_id: `img_${uuidv4()}`,
            resource_type: resourceType
        };

        const uploadStream = cloudinary.uploader.upload_stream(
            uploadOptions,
            (error, result) => {
                if (error) {
                    console.error('Cloudinary upload error:', error);
                    reject(new Error('Failed to upload file'));
                } else {
                    resolve(result.secure_url);
                }
            }
        );

        uploadStream.end(buffer);
    });
}

/**
 * Delete an image from Cloudinary by URL
 * @param {string} imageUrl - Full Cloudinary URL
 * @returns {Promise<void>}
 */
async function deleteImage(imageUrl) {
    if (!imageUrl) return;

    try {
        // Extract public_id from URL
        const urlParts = imageUrl.split('/');
        const filename = urlParts[urlParts.length - 1];
        const publicId = `${FOLDER}/${filename.split('.')[0]}`;

        await cloudinary.uploader.destroy(publicId);
    } catch (error) {
        console.error('Cloudinary delete error:', error);
    }
}

module.exports = {
    uploadImage,
    deleteImage,
    cloudinary
};
